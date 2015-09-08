These are the source files for the static documentation site hosted on GitHub Pages. The site builds with [Jekyll](http://jekyllrb.com/), using [asciidoctor](http://asciidoctor.org/) and the [asciidoctor Jekyll plugin](https://github.com/asciidoctor/jekyll-asciidoc). The theme is [jekyll-docs-template](http://bruth.github.io/jekyll-docs-template).

To get set up to build the site:

```
gem install jekyll
gem install jekyll-asciidoc
```

To see a local version of the site, run `jekyll serve`, then open a browser at <http://localhost:4000/rvi_sota_server/>.

To simply build the site without running the local server, run `jekyll build`; it will output to `./_site/`.

To update the site on Github Pages, build the site with `jekyll build`, then copy the complete generated static site to the gh-pages branch, commit, and push.

You can also just run `make` to build the site using docker. It will output the static site to `./output`.