These are the source files for the static documentation site hosted on GitHub Pages. The site builds with [Jekyll](http://jekyllrb.com/), using [pandoc](http://pandoc.org/) and a Jekyll plugin called [jekyll-pandoc-multiple-formats](https://github.com/fauno/jekyll-pandoc-multiple-formats). The theme is [jekyll-docs-template](http://bruth.github.io/jekyll-docs-template).

To get it up and running, you'll need the pandoc executable available on your system, plus the Jekyll and jekyll-pandoc-multiple-formats ruby gems:

```
gem install jekyll
git clone git@github.com:fauno/jekyll-pandoc-multiple-formats.git
cd jekyll-pandoc-multiple-formats
gem build jekyll-pandoc-multiple-formats.gemspec
gem install --local jekyll-pandoc-multiple-formats.gem
```

To see a local version of the site, run `jekyll serve`, then open a browser at <http://localhost:4000/rvi_sota_server/>.

To build the site without the server, run `jekyll build`; it will output to `./_site/`. The live version of the generated site lives on the `gh-pages` branch; it is a special orphan branch that only exists to hold the static files for the GitHub Pages site.