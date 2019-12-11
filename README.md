Aye captain ⚓️
=======

Aye captain ⚓ is a tool for automating generation of release notes from Jira tickets.

The tool takes in CSV file as input exported from Jira and the platform type of the tickets currently
Android 🤖 and iOS 🍎 is supported [but other platforms can be easily extended].

Usage
-----  

```
$  kotlinc -script Aye.kts <path-to-csv-file.csv> <platform>

Example:

$  kotlinc -script Aye.kts ./sample.csv Android

====================

Output:

Changes
✨ AJ-2013 	 Ship adaptive icon
✨ AJ-1995 	 Improve app startup time

Instrumentation
📈 AJ-1974 	 Add analytics event for app usage

```


Download
-----

1. Install `kotlin` to run from command line [read more](https://kotlinlang.org/docs/tutorials/command-line.html)
2. Download the `Aye.kts` script 
3. Run the Kotlin script [as described above]
4. Enjoy automation 🎉 

Roadmap 🛣
-----

- [ ] Support fetching ticket list from Jira server directly
- [ ] Ship it as command [example: aye ]
- [ ] Support configuring the emojis
- [ ] Post the result automatically on Slack [maybe]
- [ ] Use Kotlin `Sequence` for better performance ⚡


License
-------

    Copyright 2019 Akshay Chordiya

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.