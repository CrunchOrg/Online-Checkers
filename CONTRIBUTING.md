# Contributing

How The Nodes work in this repository. The professor grades the Git history, so it should show regular, meaningful commits from every team member, not one big push at the end.

## Branches

| Branch | Rule |
| --- | --- |
| `main` | The default branch: always builds and runs. Nobody pushes to it directly; work reaches it only through a pull request. |
| `<issue#>-short-name` | One task, branched off `main` and named with its GitHub issue number, for example `3-client-handler-threads` for issue #3. Deleted after it merges. |

The personal branches from before this setup (`aducci_dev01`, `quint_dev01`, ...) can keep going until their current work is merged; start new work on an issue branch.

To start a task:

```
git checkout main
git pull
git checkout -b 3-client-handler-threads
```

## Commits

- One logical change per commit, and commit as you go.
- Start the message with the issue number: `#3 run each ClientHandler on its own thread`
- Present tense, lowercase, no period at the end.
- Never commit build output (`target/`) or IDE files (`.idea/`, `.settings/`, `.classpath`, `.project`). The `.gitignore` already skips them.

## Pull requests

1. Push your branch and open a pull request into `main`.
2. Put `Closes #<issue>` in the description so the issue closes when it merges.
3. The **build** check must pass: it compiles the code and runs the tests with Maven.
4. One teammate other than the author reviews and approves it.
5. Merge with **Create a merge commit** (not squash), then delete the branch. This keeps every individual commit, and who made it, in the history the professor grades.

## Issues and the board

Every task is a GitHub issue on the project board, assigned to the person doing it, with a label for the part of the system it touches:

| Label | For |
| --- | --- |
| `server` | `Server`, `GameSession`, server-side game logic |
| `client` | `Client`, `GameBoard`, `BoardSquare`, the Swing GUI |
| `protocol` | `GameMessage` and the JSON messages |
| `networking` | sockets, threads, connections, disconnects |
| `testing` | tests and testing checkpoints |
| `docs` | README and `docs/` |
| `bug` | something that is broken |
| `stretch` | beyond the proposal's required objectives (spectators, leaderboard) |

All issues for the final submission go in the **Final demo** milestone.

## Building before you push

```
mvn test
```

If that fails on your machine it will fail the check on the pull request too.
