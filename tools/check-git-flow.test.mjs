import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validatePullRequest, validateTag } from './check-git-flow.mjs';

const pr = (head, base, fork = false) => ({
    head: { ref: head, repo: { full_name: fork ? 'contributor/jarful' : 'kamome-run/jarful' } },
    base: { ref: base, repo: { full_name: 'kamome-run/jarful' } },
});
for (const [head, base, fork = false] of [
    ['feature/batch-delete', 'develop'], ['feature/print/protocol', 'develop', true],
    ['release/1.8.0', 'main'], ['release/1.8.0', 'develop'],
    ['hotfix/1.7.5', 'main'], ['hotfix/1.7.5', 'develop'],
    ['hotfix/1.7.5', 'release/1.8.0'], ['main', 'develop'],
]) test(`accept ${fork ? 'fork:' : ''}${head} -> ${base}`, () => assert.doesNotThrow(() => validatePullRequest(pr(head, base, fork))));

for (const [head, base, fork = false] of [
    ['feature/batch-delete', 'main'], ['develop', 'main'], ['fix/printer', 'develop'],
    ['release/01.8.0', 'main'], ['release/1.8', 'main'], ['hotfix/bug', 'main'],
    ['feature/', 'develop'], ['main', 'develop', true],
    ['release/1.8.0', 'main', true], ['hotfix/1.7.5', 'main', true],
    ['feature/change', 'release/1.8.0'], ['develop', 'release/1.8.0'],
    ['feature/change', 'unknown'],
]) test(`reject ${fork ? 'fork:' : ''}${head} -> ${base}`, () => assert.throws(() => validatePullRequest(pr(head, base, fork))));

test('release tag matches all three app versions and main ancestry', () => validateTag('v1.8.0', ['1.8.0', '1.8.0', '1.8.0'], true));
test('reject release not integrated into main', () => assert.throws(() => validateTag('v1.8.0', ['1.8.0'], false)));
test('reject mismatched app versions', () => assert.throws(() => validateTag('v1.8.0', ['1.8.0', '1.7.4'], true)));
test('reject non-release or malformed tag', () => {
    for (const tag of ['1.8.0', 'v01.8.0', 'v1.8.0-rc.1', 'vlatest']) assert.throws(() => validateTag(tag, ['1.8.0'], true));
});
