# Code Release Process
1. Create a new branch named release_VERSION, where VERSION is the version number for the release.
2. Check out that branch
3. Update the version number in pom.xml
3. Ensure that everything builds ok using:
```
   mvn clean package site
```
4. Update the release notes in src\site\markdown\RELEASE_NOTES.md
5. Update the files in src\site\markdown to point to the correct branch for the release
   This should just be a simple search and replace operation.
6. Commit these changes
7. Verify the build one more time after making these changes
```
   mvn clean package site
```
8. Test the documentation created in target\site
   1. Verify that all links work
   2. Verify that all content points to the right version in GitHub
9. ZIP the documentation site using 7Z, WINZIP, jar or other archiving utility
```
   7z a extract-validator-site.zip site
```
 -OR-
```
  jar -M -c -f extract-validator-site-1.0.0betax.zip site
```
10. Rename the following files in the target folder:
* extract-validator.jar extract-validator-VERSION.jar
* extract-validator-javadoc.jar extract-validator-javadoc-VERSION.jar
* extract-validator-sources.jar extract-validator-sources-VERSION.jar
* extract-validator-site.zip extract-validator-site-VERSION.zip

11. Create a New Release in GitHub named VERSION where VERSION is the version number for the release
12. Set the tag to VERSION for the release
13. Add the renamed files from step 10 to the release
14. Copy the contents of src/site/markdown/RELEASE_NOTES.md to the Release Notes in GitHub
15. Publish the Release
16. Bask in glory

