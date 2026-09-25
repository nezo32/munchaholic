<!-- The PR title becomes a release-notes line: write it as an imperative sentence ("Add Golden Carrot Pie food"). -->

## Summary

<!-- What changes and why, in 1-5 lines. Link issues with "Closes #123". -->

## Type

<!-- Set by the branch prefix (feature/ fix/ chore/ docs/ ...). Add the `breaking` label for breaking changes, `skip-changelog` to leave this PR out of the notes. -->
- [ ] Feature  - [ ] Fix  - [ ] Chore / CI / docs  - [ ] Breaking change

## Testing

<!-- How you checked it: tests added, `./gradlew build`, `./gradlew build -Pmc=26.2`, in-game steps. -->

## Checklist

- [ ] Branch is named `<type>/<kebab-name>` and targets `main`
- [ ] Local checks pass (`cd fabric && ./gradlew build` and `./gradlew build -Pmc=26.2`)
- [ ] No hand-edited versions (`mod_version`): the version comes from the release tag
- [ ] Docs updated if behaviour or workflows changed
