const { spawnSync } = require('node:child_process');
const readline = require('node:readline/promises');
const { stdin: input, stdout: output } = require('node:process');

const commitTypes = [
    '✨ Feat',
    '📦 Chore',
    '💄 Design',
    '🚨 Fix',
    '🎨 Style',
    '⚡ Perf',
    '🔥 Remove',
    '🚀 Release',
    '🎉 Init',
    '✅ Test',
    '🔒 Security',
    '♻ Refactor',
    '🔨 Modify',
    '🚚 Rename',
    '📝 Docs',
    '➖ Remove',
    '🔖 Release',
    '🧪 Test',
    '🚑 Hotfix',
];

function hasStagedChanges() {
    const result = spawnSync('git', ['diff', '--cached', '--quiet']);

    if (result.error) {
        throw new Error('Git 상태를 확인할 수 없습니다. Git 저장소에서 실행해주세요.');
    }

    return result.status === 1;
}

async function selectCommitType(rl) {
    console.log('\n커밋 타입을 선택하세요.');
    commitTypes.forEach((commitType, index) => {
        console.log(`  ${index + 1}. ${commitType}`);
    });

    while (true) {
        const answer = await rl.question('\n번호: ');
        const selectedIndex = Number.parseInt(answer, 10) - 1;

        if (Number.isInteger(selectedIndex) && commitTypes[selectedIndex]) {
            return commitTypes[selectedIndex];
        }

        console.log(`1부터 ${commitTypes.length} 사이의 번호를 입력해주세요.`);
    }
}

async function main() {
    if (!hasStagedChanges()) {
        console.error('스테이징된 변경 사항이 없습니다. 먼저 git add <파일>을 실행해주세요.');
        process.exitCode = 1;
        return;
    }

    const rl = readline.createInterface({ input, output });

    try {
        const type = await selectCommitType(rl);
        const subject = (await rl.question('커밋 제목: ')).trim();

        if (!subject) {
            console.error('커밋 제목은 비워둘 수 없습니다.');
            process.exitCode = 1;
            return;
        }

        const body = (await rl.question('상세 설명 (선택): ')).trim();
        const commitArguments = ['commit', '-m', `${type}: ${subject}`];

        if (body) {
            commitArguments.push('-m', body);
        }

        const result = spawnSync('git', commitArguments, { stdio: 'inherit' });
        process.exitCode = result.status ?? 1;
    } finally {
        rl.close();
    }
}

main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
});
