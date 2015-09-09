all: 
	mkdir -p output
	docker build -t sota_docs_site .
	docker run -t -i -v $(PWD)/output:/data/output sota_docs_site jekyll build -d output

clean:
	rm -rf output

.PHONY: all clean