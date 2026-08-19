# GitHub Actions + AWS SSM 자동 배포

공개 레포에서 EC2에 self-hosted runner를 설치하지 않고 자동 배포하는 방법이다. GitHub-hosted runner가 GitHub OIDC로 짧은 수명의 AWS 자격 증명을 발급받고, Systems Manager Run Command로 EC2에 배포 명령을 전달한다. AWS 액세스 키, EC2 SSH 개인 키, 22번 포트 공개가 필요 없다.

워크플로는 `dev` 브랜치에 push된 경우와 수동 실행에서만 배포된다. PR에는 실행되지 않는다.

## 1. EC2를 SSM 관리형 노드로 등록

1. IAM에서 `JaedaeroEc2SsmRole` 역할을 생성한다. 신뢰할 주체는 **EC2**로 선택한다.
2. 해당 역할에 AWS 관리형 정책 `AmazonSSMManagedInstanceCore`를 연결한다.
3. EC2 콘솔에서 대상 인스턴스를 선택하고 **작업 → 보안 → IAM 역할 수정**으로 들어가 이 역할을 연결한다.
4. AWS Systems Manager → Fleet Manager → Managed nodes에서 인스턴스가 `Online`인지 확인한다.

Ubuntu 이미지에는 SSM Agent가 포함될 수 있다. 다음으로 서비스 상태를 확인할 수 있다.

```bash
sudo snap services amazon-ssm-agent
```

## 2. GitHub OIDC 공급자와 배포 역할 생성

IAM → Identity providers에서 공급자가 없다면 다음 값으로 만든다.

- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

그다음 `JaedaeroGitHubDeployRole` 역할을 생성하고, **Web identity** 신뢰 정책을 다음과 같이 제한한다. 이 레포는 2026-07-15 이전에 생성되어 기본 subject 형식을 사용한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:BellongBellong/Jaedaero_backend:ref:refs/heads/dev"
        }
      }
    }
  ]
}
```

역할 권한에는 다음 인라인 정책을 붙인다. 대상 인스턴스에 `DeploymentTarget=jaedaero-production` 태그를 먼저 추가해야 한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript"
    },
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:instance/*",
      "Condition": {
        "StringEquals": {
          "ssm:resourceTag/DeploymentTarget": "jaedaero-production"
        }
      }
    },
    {
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }
  ]
}
```

`AWS-RunShellScript` 권한은 EC2에서 관리자 권한 명령을 실행할 수 있다. 따라서 subject를 `dev` 브랜치로 정확히 제한하고, `dev`에는 PR 리뷰 후에만 머지되도록 보호 규칙을 설정한다.

## 3. GitHub Actions 변수 등록

GitHub 레포 → Settings → Secrets and variables → Actions → **Variables**에 아래 두 값을 추가한다.

| 변수 | 값 |
| --- | --- |
| `AWS_DEPLOY_ROLE_ARN` | `JaedaeroGitHubDeployRole`의 ARN |
| `EC2_INSTANCE_ID` | 대상 EC2의 `i-...` 인스턴스 ID |

AWS 액세스 키나 SSH 개인 키는 등록하지 않는다.

## 4. 동작 확인

이 문서와 `.github/workflows/deploy-ssm.yml`을 `dev`에 머지하면 Actions 탭에서 **Deploy to EC2 via SSM**을 수동 실행할 수 있다. 성공한 뒤부터는 `dev` push마다 자동 실행된다.

배포 명령은 `/opt/jaedaero/app`에서 `origin/dev`를 받아 Docker Compose를 다시 빌드하고, `/swagger-ui.html` 응답까지 확인한다. MySQL Docker 볼륨과 `/opt/jaedaero/config`의 비밀 설정 파일은 삭제하거나 Git에 올리지 않는다.
