Provide a code review for the given pull request.

## Project Review Guidelines

- All review comments, PR summaries, and feedback must be written in **Korean**.
- This project is **Java 17 + Spring Boot**.
- Judge project conventions/rules against the root `CLAUDE.md`
  (package layout / use-case services / error handling / entities & persistence / style).

### Review principles

- Explain **why** something is a problem from a maintainability/scalability/stability
  standpoint.
- Whenever you flag a problem, provide a **fix direction or example code** wherever possible.
- Post **every** issue you find regardless of severity (including LOW-level style/preference
  issues).
- Prefix each comment's first line with a severity tag: `[BLOCKER]` / `[HIGH]` / `[MEDIUM]` /
  `[LOW]`.

### Comment format (readability)

Don't write one long paragraph. Use subheadings and blank lines in this structure instead:

```
[SEVERITY] One-line summary

**Problem**: what it is and why it matters (1-2 sentences)

**Evidence**: reproduction condition, referenced code/docs

**Fix direction**: concrete fix

​```java
// fix example
​```
```

- Keep each section to 3 sentences or fewer.
- Put code/config values in backticks or a code block, never inline in prose.
- Add a blank line before and after each subheading (`**Problem**`, `**Evidence**`,
  `**Fix direction**`).

### Severity criteria

- **BLOCKER**: must be fixed before merge (security vulnerability, crash, possible data loss)
- **HIGH**: fix strongly recommended (logic error, wrong result)
- **MEDIUM**: fix recommended (maintainability concern, stability risk, transaction boundary
  issue)
- **LOW**: optional improvement (naming, idiom, style)

## Base Procedure (from the original code-review command)

Source: ~/.claude/plugins/marketplaces/claude-plugins-official/plugins/code-review/commands/code-review.md
The original steps below are unchanged. Project-specific instructions for this repo are marked
inline as blockquotes, using one of two tags:

- `[EXTEND]` — an extra constraint layered on top of the step; it does not conflict with what
  the step already says.
- `[OVERRIDE]` — replaces or changes what the step says to do.

> **[EXTEND]** Parse the target repository from `$ARGUMENTS` (format: `owner/repo/pull/number`).
> **[EXTEND]** Assume every tool works correctly (for all agents and
> subagents) — never make exploratory or test tool calls, only call a tool when it's actually
> needed to finish the task.

To do this, follow these steps precisely:

1. Use a Haiku agent to check if the pull request (a) is closed, (b) is a draft, (c) does not need a code review (eg. because it is an automated pull request, or is very simple and obviously ok), or (d) already has a code review from you from earlier. If so, do not proceed.
2. Use another Haiku agent to give you a list of file paths to (but not the contents of) any relevant CLAUDE.md files from the codebase: the root CLAUDE.md file (if one exists), as well as any CLAUDE.md files in the directories whose files the pull request modified
3. Use a Haiku agent to view the pull request, and ask the agent to return a summary of the change
4. Then, launch 5 parallel Sonnet agents to independently code review the change. The agents should do the following, then return a list of issues and the reason each issue was flagged (eg. CLAUDE.md adherence, bug, historical git context, etc.):
   a. Agent #1: Audit the changes to make sure they compily with the CLAUDE.md. Note that CLAUDE.md is guidance for Claude as it writes code, so not all instructions will be applicable during code review.
   b. Agent #2: Read the file changes in the pull request, then do a shallow scan for obvious bugs. Avoid reading extra context beyond the changes, focusing just on the changes themselves. Focus on large bugs, and avoid small issues and nitpicks. Ignore likely false positives.
   c. Agent #3: Read the git blame and history of the code modified, to identify any bugs in light of that historical context
   d. Agent #4: Read previous pull requests that touched these files, and check for any comments on those pull requests that may also apply to the current pull request.
   e. Agent #5: Read code comments in the modified files, and make sure the changes in the pull request comply with any guidance in the comments.

   > **[EXTEND]** Never call `ScheduleWakeup` while waiting on these parallel
   > agents (or the ones in step 5). If any agent hasn't returned yet, call `Bash("sleep 15")`
   > and repeat until all results arrive (up to 10 times). ScheduleWakeup has no CI runtime to
   > resume the session, so it would end the session immediately.

5. For each issue found in #4, launch a parallel Haiku agent that takes the PR, issue description, and list of CLAUDE.md files (from step 2), and returns a score to indicate the agent's level of confidence for whether the issue is real or false positive. To do that, the agent should score each issue on a scale from 0-100, indicating its level of confidence. For issues that were flagged due to CLAUDE.md instructions, the agent should double check that the CLAUDE.md actually calls out that issue specifically. The scale is (give this rubric to the agent verbatim):
   a. 0: Not confident at all. This is a false positive that doesn't stand up to light scrutiny, or is a pre-existing issue.
   b. 25: Somewhat confident. This might be a real issue, but may also be a false positive. The agent wasn't able to verify that it's a real issue. If the issue is stylistic, it is one that was not explicitly called out in the relevant CLAUDE.md.
   c. 50: Moderately confident. The agent was able to verify this is a real issue, but it might be a nitpick or not happen very often in practice. Relative to the rest of the PR, it's not very important.
   d. 75: Highly confident. The agent double checked the issue, and verified that it is very likely it is a real issue that will be hit in practice. The existing approach in the PR is insufficient. The issue is very important and will directly impact the code's functionality, or it is an issue that is directly mentioned in the relevant CLAUDE.md.
   e. 100: Absolutely certain. The agent double checked the issue, and confirmed that it is definitely a real issue, that will happen frequently in practice. The evidence directly confirms this.
6. Filter out any issues with a score less than 80. If there are no issues that meet this criteria, do not proceed.

   > **[OVERRIDE]** Per "Review principles" above, this project posts every validated issue
   > regardless of severity, including LOW-level style nitpicks. Apply this filter only to
   > drop false positives / unverified issues — never drop an issue for being low severity
   > alone.

7. Use a Haiku agent to repeat the eligibility check from #1, to make sure that the pull request is still eligible for code review.
8. Finally, use the gh bash command to comment back on the pull request with the result. When writing your comment, keep in mind to:
   a. Keep your output brief
   b. Avoid emojis
   c. Link and cite relevant code, files, and URLs

   > **[OVERRIDE]** Replace this step: post inline comments attached to file/line instead of a
   > single summary comment:
   > ```bash
   > REPO="<owner>/<repo>"   # parsed from $ARGUMENTS
   > PR_NUMBER="<number>"    # parsed from $ARGUMENTS
   > COMMIT_SHA=$(gh pr view $PR_NUMBER --repo $REPO --json headRefOid -q .headRefOid)
   >
   > gh api repos/$REPO/pulls/$PR_NUMBER/comments \
   >   -f body="$(cat <<'BODY'
   > [SEVERITY] One-line summary (follow the "Comment format" section above)
   > BODY
   > )" \
   >   -f commit_id="$COMMIT_SHA" \
   >   -f path="src/relative/path/to/File.java" \
   >   -F line=LINE_NUMBER \
   >   -f side="RIGHT"
   > ```
   > - Line number: use the new-file line number for lines with a `+` in the diff.
   > - Never post a duplicate comment for the same issue.
   > - If inline posting fails, fall back to a single `gh pr comment` with the full issue list.

Examples of false positives, for steps 4 and 5:

- Pre-existing issues
- Something that looks like a bug but is not actually a bug
- Pedantic nitpicks that a senior engineer wouldn't call out
- Issues that a linter, typechecker, or compiler would catch (eg. missing or incorrect imports, type errors, broken tests, formatting issues, pedantic style issues like newlines). No need to run these build steps yourself -- it is safe to assume that they will be run separately as part of CI.
- General code quality issues (eg. lack of test coverage, general security issues, poor documentation), unless explicitly required in CLAUDE.md
- Issues that are called out in CLAUDE.md, but explicitly silenced in the code (eg. due to a lint ignore comment)
- Changes in functionality that are likely intentional or are directly related to the broader change
- Real issues, but on lines that the user did not modify in their pull request

Notes:

- Do not check build signal or attempt to build or typecheck the app. These will run separately, and are not relevant to your code review.
- Use `gh` to interact with Github (eg. to fetch a pull request, or to create inline comments), rather than web fetch
- Make a todo list first
- You must cite and link each bug (eg. if referring to a CLAUDE.md, you must link it)
- When linking to code, follow the following format precisely, otherwise the Markdown preview won't render correctly: https://github.com/anthropics/claude-cli-internal/blob/c21d3c10bc8e898b7ac1a2d745bdc9bc4e423afe/package.json#L10-L15
  - Requires full git sha
  - You must provide the full sha. Commands like `https://github.com/owner/repo/blob/$(git rev-parse HEAD)/foo/bar` will not work, since your comment will be directly rendered in Markdown.
  - Repo name must match the repo you're code reviewing
  - # sign after the file name
  - Line range format is L[start]-L[end]
  - Provide at least 1 line of context before and after, centered on the line you are commenting about (eg. if you are commenting about lines 5-6, you should link to `L4-7`)

ARGUMENTS: $ARGUMENTS
