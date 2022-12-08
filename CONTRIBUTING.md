# Contributing to Gaia Sky

First of all, thanks for reading this! It means you are considering to contribute to Gaia Sky, which is appreciated.

## How to contribute

There are several ways to contribute to the Gaia Sky project:

### Merge requests and source code

Start by checking out the official documentation ([here](https://gaia.ari.uni-heidelberg.de/gaiasky/docs)) to get acquainted with the project. It may also help decide what part you actually want to contribute to. Merge requests should be accompanied with extensive and comprehensive comments. In case that changes in the documentations are needed, a new merge request should be created in the [documentation project](https://codeberg.org/gaiasky/gaiasky-docs).

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

The translation files are in `assets/i18n/gsbundle[lang-code].properties` for the main UI text strings, and `assets/i18n/objects[lang-code].properties` for object names.

In order to add a new translation, just copy the default files and use the code of your language and optionally your country (check [here](https://saimana.com/list-of-country-locale-code/) to find out your code). Files without language code correspond to English (GB).

For example, if you want to create a translation for Vietnamese (Vietnam), with language code `vi_VN`, copy the default English files to the Vietnamese like this:

```bash
cd assets/i18n
cp gsbundle.properties assets/i18n/gsbundle_vi_VN.properties
cp objects.properties assets/i18n/objects_vi_VN.properties
```

Now you are ready to start translating! Once you are done, create a merge request and we'll merge your file into the main repository after making sure everything is alright.

#### Tip strings

The loading screen of Gaia Sky displays tips on the general working of the program at the bottom.

These tips are in the main properties bundle, under the keys `tip.n`, where `n` is a number from 0 to 100. They are optional and have a special format. They can have up to three regions separated by `|`. Each region may start with a style definition, using the special string `%%`. Additionally, actions `action.*` are converted to their bound keys automatically.

These tips are defined in the `gsbundle[lang].properties` files, under the keys `tip.[index]`.

Each tip is a sequence of strings, or groups, separated by the character `|`. For example,
`first group|second group|third group` is a tip with three groups, processed separately.
Each group optionally defines the label style to use by prefixing `%%`, followed by the
style name. For instance, `%%mono-big here is a text` would print `here is a text` using the
label style `mono-big`.
Additionally, styles can be followed by an action ID, which is converted to the keyboard mappings.
For instance, `%%mono-big action.close` would print :guilabel:`Esc` in the `mono-big` label style. The
key mappings are separated by `+` and given each a style separately.

Additionally, groups can also define images stored in the default skin
as drawables. To include an image, use the prefix `$$`, followed by the identifier of the
image to include. For example, use `$$gamepad-a` to include an image of the A gamepad button.
Images need to live in their own group. The rest of the content of the group is ignored.

#### Funny strings

The loading screen of Gaia Sky displays funny sentences which are automatically generated from a set of verbs, adjectives and objects (nouns). These are in the main bundle, under the keys `funny.verb.n`, `funny.adjective.n` and `funny.object.n`, where `n` are numbers from 0 to 100. Additionally, there is an order key called `funny.order` which specifies the order in which the verbs, adjectives and objects must be combined for the language in question to create well-constructed sentences, using `V` for verbs, `A` for adjectives and `O` for objects. In English, for instance, the order is verb + adjective + object, so the key reads `funny.order=V A O`.

#### Translation status

Right now we have translation files for Bulgarian, English (UK and US), German, French, Catalan, Spanish and Slovenian. The level of completion varies from language to language. You can compute it by running:

```bash
gradlew core:runTranslationStatus
```

The status is only checked for the main `gsbundle[lang-code].properties` file, as the objects file can't be complete because the number of objects that may, at some point, be loaded into Gaia Sky is infinite.

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

```bash
gradlew core:runTranslationStatus --args='-s -u'
```

The status of the different translations as of 2022-11-30 is as follows.

```
Total keys: 1413

Catalan (ca)
Translated: 1413/1413
100.0%

Spanish (Spain) (es_ES)
Translated: 1413/1413
100.0%

German (Germany) (de_DE)
Translated: 578/1413
40.91%

French (France) (fr_FR)
Translated: 450/1413
31.85%

Slovenian (Slovenia) (sl_SI)
Translated: 359/1413
25.41%

Bulgarian (Bulgaria) (bg_BG)
Translated: 1344/1413
95.12%

Russian (Russia) (ru_RU)
Translated: 758/1413
53.64%
```

### Formatting properties files

The default English properties file has lots of comments, which sometimes are not kept with some i18n editors. You can recover the comments in your translation file (for `language_code` and `country_code`) by running our `I18nFormatter`:

```bash
gradlew core:runI18nFormatter --args="gsbundle.properties gsbundle_<lang_code>[<_country_code>].properties"
```

This updates your translation file with the undetected keys from the base English file, and includes all comments.

### Data

Contributing data files is always welcome. Have a look at the data files for other datasets in our [data repository](https://gaia.ari.uni-heidelberg.de/gaiasky/repository/). Also, you might want to have a look at the [dataset format documentation in the official docs](https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html).


