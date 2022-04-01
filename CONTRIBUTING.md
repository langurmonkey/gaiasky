# Contributing to Gaia Sky

First of all, thanks for reading this! It means you are considering to contribute to Gaia Sky, which is appreciated.

## How to contribute

There are several ways to contribute to the Gaia Sky project:

### Merge requests and source code

Start by checking out the official documentation ([here](https://gaia.ari.uni-heidelberg.de/gaiasky/docs)) to get acquainted with the project. It may also help decide what part you actually want to contribute to. Merge requests should be accompanied with extensive and comprehensive comments. In case that changes in the documentations are needed, a new merge request should be created in the [documentation project](https://gitlab.com/gaiasky/gaiasky-docs).

Merge requests should never contain configuration files unless totally necessary (do not commit your `conf/global.properties`). Also, make sure that the project compiles and all the dependencies are well specified in the `build.gradle` file. 

The code style template is available in the root of the project in the IntelliJ IDEA format: [gaiasky.codestyle.xml](gaiasky.codestyle.xml).

### Commit message format

Gaia Sky adheres to a standard commit message format that should be kept in order to generate meaningful changelogs:

```
<type>: <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

Commit message example:

```
feat: adds relativistic camera mode

Add relativistic camera mode which makes use of the already implemented relativistic aberration and gravitational wave model.

Fixes #123
```

#### type

-  **feat**: new feature or improvement
-  **fix**: bug fix, should possibly reference the issue id in footer or body
-  **docs**: changes to the documentation (README, ACKNOWLEDGEMENTS, etc.)
-  **style**: changes that don't affect functionality or such as cosmetic changes or formatting
-  **refactor**: code refactorings which do not modify functionality or fix a bug, class changes, name changes, moves, etc.
-  **perf**: changes that improve performance
-  **build**: changes to the build and continuous integration systems, or to run scripts and installer files
-  **none**: minor changes that will not appear in the changelog, use for everything else or partial, non-finished commits

#### subject

Contains a condensed description of the change. Uses imperative present tense (change and add instead of changes/changed or adds/added). It shouldn't be capitalized and without a period at the end.

#### body

Thorough description of the change. You can elaborate at will. Not required.

#### footer

Cites any issues that the commit closes. Not required.

### Bug reports and requests

Issues are the way to go.

If reporting bugs and crashes, provide a report as extensive as possible, including (if applicable):

- A description of the problem
- How to reproduce
- Your system (CPU, RAM, GPU, OS, OS version, graphics drivers version, etc.)
- A stack trace if applicable.

A stack trace can be obtained by simply copy-pasting the contents of the terminal window (if launched from terminal) or in the installation folder, files `output.log` and `error.log`, if launched using any of the packaged versions.

### Translations

Right now we have translation files for Bulgarian, English (UK and US), German, French, Catalan, Spanish and Slovenian. The level of completion varies from language to language. You can compute it by running

```commandline
gradlew core:runTranslationStatus
```

There are some CLI arguments you can use:

```
Usage: translationstatus [options]
  Options:
    -h, --help
      Show program options and usage information.
    -s, --show-untranslated
      Show untranslated keys for each language.
      Default: false
    -u, --show-unknown
      Show unknown keys for each language.
      Default: false
```

You can pass CLI arguments to the application via Java like this:

```commandline
gradlew core:runTranslationStatus --args='-s -u'
```

The status of the different translations as of 2022-03-31 is as follows.

```commandline
Total keys: 1195

Spanish (Spain) (es_ES)
Translated: 629/1195
52.64%

Slovenian (Slovenia) (sl_SI)
Translated: 392/1195
32.8%

French (France) (fr_FR)
Translated: 489/1195
40.92%

Russian (Russia) (ru_RU)
Translated: 809/1195
67.7%

Catalan (ca)
Translated: 1195/1195
100.0%

Bulgarian (Bulgaria) (bg_BG)
Translated: 1193/1195
99.83%

German (Germany) (de_DE)
Translated: 600/1195
50.21%

English (United States) (en_US)
Translated: 11/1195
0.92%
```

You can contribute to those, or you can create a new translation for another language. Adding new translations is as easy as submitting a pull request. Translation files should go in the [i18n](assets/i18n) folder.
First, copy the default [gsbundle.properties](assets/i18n/gsbundle.properties) file and use it as a template. The translation files must have the format:

`gsbundle_<language_code>[<_country_code>].properties`

### Data

Contributing data files is always welcome. Have a look at the current data files in the [data](assets/data) folder, most of them should be pretty self-explanatory. Also, you might want to have a look at the documentation on [data files and format](https://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest/Data-catalogs-formats.html).


