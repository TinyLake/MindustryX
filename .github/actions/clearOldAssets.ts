import {getOctokit} from "@actions/github";

// Bun 使用 process.env 获取环境变量
const TOKEN = process.env.TOKEN;
const REPOSITORY = process.env.REPOSITORY;
const SELF_BUILD = process.env.RELEASE_VERSION; // 当前构建的版本，防止误删
const DRY_RUN = process.env.DRY_RUN || false;

if (!TOKEN || !REPOSITORY) {
    console.error("Missing TOKEN or REPOSITORY env");
    process.exit(1);
}

const octokit = getOctokit(TOKEN) as import("@octokit/plugin-rest-endpoint-methods/dist-types/types").Api;
const [owner, repo] = REPOSITORY.split("/");

/**
 * 核心逻辑：清理 Pre-releases
 * 条件：是 Pre-release 且不包含当前正在构建的版本，只保留最新的 3 个
 */
async function clearOldPreReleases(keep: number = 3) {
    console.log(`Checking releases in ${owner}/${repo}...`);

    // 1. 获取所有 releases
    const {data: releases} = await octokit.rest.repos.listReleases({
        owner,
        repo,
        per_page: 100
    });

    // 2. 筛选出满足条件的 pre-release (排除当前 selfBuild)
    const preReleases = releases
        .filter(r => r.prerelease && r.tag_name.startsWith("prerelease-"))
        .filter(r => r.tag_name !== SELF_BUILD)
        // 按创建时间倒序排序
        .sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());

    console.log(`Found ${preReleases.length} pre-releases.`, preReleases.map(it => it.tag_name))

    const toRemove = preReleases.slice(keep);
    if(toRemove.length === 0) {
        console.log("No clean up needed.");
        return;
    }
    console.log("Removing:", toRemove.map(r => r.tag_name));

    if (DRY_RUN) {
        console.log("Dry run mode - no deletions will be made.");
        return;
    }

    // 3. 执行删除 (同时会删除关联的 Tag)
    for (const rel of toRemove) {
        try {
            // 删除 Release
            await octokit.rest.repos.deleteRelease({
                owner,
                repo,
                release_id: rel.id
            });

            // 删除 Git Tag (由于 API 限制，需手动删除 ref)
            await octokit.rest.git.deleteRef({
                owner,
                repo,
                ref: `tags/${rel.tag_name}`
            });

            console.log(`Successfully deleted ${rel.tag_name}`);
        } catch (e: any) {
            console.error(`Failed to delete ${rel.tag_name}: ${e.message}`);
        }
    }
}

await clearOldPreReleases();