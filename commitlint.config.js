module.exports = {
  extends: ['@commitlint/config-conventional'],
  parserPreset: {
    parserOpts: {
      headerPattern: /^(?<type>.+?)\s*:\s*(?<subject>.+)$/,
      headerCorrespondence: ['type', 'subject'],
    },
  },
  rules: {
    'subject-empty': [2, 'never'],
    'subject-full-stop': [2, 'never', '.'],
    'type-case': [0],
    'type-empty': [2, 'never'],
    'type-enum': [
      2,
      'always',
      [
        '✨ Feat',
        '📦️ Chore',
        '💄 Design',
        '🐛 Fix',
        '🎨 Style',
        '♻️ Refactor',
        '🔨 Refactor',
        '🚚 Rename',
        '📝 Docs',
        '➖ Remove',
        '🔖 Release',
        '🧪 Test',
        '🚑️ Hotfix',
        '🔨 Modify',
      ],
    ],
  },
};
