FROM ruby:2.2.3

RUN gem install asciidoctor
RUN gem install jekyll
RUN gem install jekyll-asciidoc
RUN gem install therubyracer

WORKDIR /data

ADD . /data
