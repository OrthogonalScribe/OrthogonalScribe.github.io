#!/usr/bin/env bash

set -ex

N=2; E=2; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E

N=2; E=300; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=17; E=300; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=150; E=300; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=300; E=300; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E

N=2; E=17576; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=132; E=17576; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=8788; E=17576; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
N=17576; E=17576; (echo $E; ./genEulerianGraph.sc $N $E) > in/$N.$E
