#!/usr/bin/env bash

set -e

prog=$1
echo prog=$prog

# TODO: loop over in/*
for test in leetcode.1 leetcode.2 2.2 2.300 17.300 300.300 2.17576 132.17576 8788.17576 17576.17576
do
	echo -ne "\t- $test\t"
	if $(cmp -s <($prog in/$test) expected/$test)
	then echo OK
	else echo NOK
	fi
done | column -t
