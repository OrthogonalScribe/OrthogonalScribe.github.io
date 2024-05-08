#!/usr/bin/env bash

set -ex

./localSort in/2.2 > expected/2.2

./localSort in/2.300 > expected/2.300
./localSort in/17.300 > expected/17.300
./localSort in/150.300 > expected/150.300
./localSort in/300.300 > expected/300.300

./localSort in/2.17576 > expected/2.17576
./localSort in/132.17576 > expected/132.17576
./localSort in/8788.17576 > expected/8788.17576
./localSort in/17576.17576 > expected/17576.17576
