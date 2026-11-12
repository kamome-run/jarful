import { readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const version = '(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)';
const release = new RegExp(`^release/${version}$`);
const hotfix = new RegExp(`^hotfix/${version}$`);
const feature = /^feature\/[a-z0-9][a-z0-9._/-]*$/;

export function validatePullRequest(pr) {
    const base = pr.base.ref;
    const head = pr.head.ref;
    const internal = pr.base.repo.full_name === pr.head.repo?.full_name;
    const integrated = internal && (release.test(head) || hotfix.test(head));
    if (base === 'main' && integrated) return;
    if (base === 'develop' && (feature.test(head) || integrated || (internal && head === 'main'))) return;
    if (release.test(base) && internal && hotfix.test(head)) return;
    throw new Error(`Git-flow rejects ${head} -> ${base}. See docs/GIT_FLOW.md.`);
}

export function validateTag(tag, versions, onMain) {
    if (!new RegExp(`^v${version}$`).test(tag)) throw new Error('Release tags must be vMAJOR.MINOR.PATCH.');
    if (!onMain) throw new Error('The release commit must be reachable from origin/main.');
    if (versions.some(value => value !== tag.slice(1))) throw new Error(`Version mismatch for ${tag}: ${versions.join(', ')}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    if (process.argv[2] === '--tag') {
        const git = (...args) => execFileSync('git', args, { encoding: 'utf8' }).trim();
        const contents = path => readFileSync(path, 'utf8');
        const value = (path, pattern) => {
            const match = contents(path).match(pattern);
            if (!match) throw new Error(`Cannot read version from ${path}`);
            return match[1];
        };
        let onMain = false;
        try { git('merge-base', '--is-ancestor', 'HEAD', 'refs/remotes/origin/main'); onMain = true; } catch {}
        validateTag(process.env.GITHUB_REF_NAME ?? '', [
            value('androidApp/build.gradle.kts', /versionName\s*=\s*"([^"]+)"/),
            value('desktopApp/build.gradle.kts', /packageVersion\s*=\s*"([^"]+)"/),
            value('shared/src/commonMain/kotlin/dev/jarful/AppVersion.kt', /const val NAME\s*=\s*"([^"]+)"/),
        ], onMain);
    } else {
        const event = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
        if (!event.pull_request) throw new Error('A pull request event is required.');
        validatePullRequest(event.pull_request);
    }
    console.log('Git-flow validation passed.');
}
