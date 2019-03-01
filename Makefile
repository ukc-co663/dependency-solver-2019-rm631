all: compile

compile: deps
	./compile.sh

deps:
	RUN chmod +x ./install_deps.sh
	touch deps

test: compile
	./run_tests.sh

clean:
	rm -rf classes

reallyclean: clean
	rm -rf lib deps

.PHONY: all compile test clean reallyclean
