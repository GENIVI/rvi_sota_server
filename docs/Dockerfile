FROM ruby:2.2.5

ADD Gemfile /

RUN bundle install
VOLUME /site
WORKDIR /site
ENTRYPOINT ["jekyll"]
CMD ["b"]

