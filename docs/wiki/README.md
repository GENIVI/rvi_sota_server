# SOTA Documentation Wiki

This directory contains the source files for the SOTA project documentation. Because it is an open source project freely available on github, we want to keep up-to-date documentation available directly on the project's wiki page. However, there are some unfortunate limitations imposed by github's wiki functionality:

* Doesn't support pull requests
* Doesn't allow embedding of various kinds of useful files, like SVGs

Additionally, some of our documentation resources make a lot more sense to store and edit as spreadsheets (in CSV format), but we still want them to display as regular embedded tables in the wiki.

To solve these problems, the files for the wiki are kept here in the docs/wiki folder. To update documentation, clone the project, create a feat/wiki/yourchanges branch, change what needs to be changed, and submit a pull request. When the changes are merged, we run a script to sync them over to the wiki.

## Resource files

Currently, we have resources in two formats that are not directly supported by github's wiki and need to be converted. Diagrams are created as SVG files, then converted to PNG to sync to the wiki. Markdown files that contain features that are not supported by github-flavoured markdown by default (and thus must be run through pandoc before syncing to the wiki) are given the extension `.pdmd`.