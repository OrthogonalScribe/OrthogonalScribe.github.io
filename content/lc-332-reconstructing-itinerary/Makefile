CXXFLAGS := -std=c++20 $(OPTFLAGS)
LDLIBS := -lbenchmark

TARGETS := benchmark globalSort localSort make_heap minPriQ multiset

.PHONY: clean

all: $(TARGETS)

%: %.cpp
	$(CXX) $(CXXFLAGS) $^ $(LDLIBS) -o $@

clean:
	rm -f $(TARGETS)
