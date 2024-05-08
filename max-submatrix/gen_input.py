#!/usr/bin/env python3

import argparse
import random
import sys

parser = argparse.ArgumentParser(
    description='Input generator for the maximum NzCSS problem')

parser.add_argument('M', type=int, help='Number of rows of the input matrix')
parser.add_argument('N', type=int, help='Number of columns of the input matrix')
parser.add_argument('seed', type=int, help='Seed for the RNG')
parser.add_argument('zero_prob', type=int, choices=range(0,101), metavar="[0-100]",
                    help='Probability of an element being zero')
parser.add_argument('--max_val', type=int,
                    help='Optional maximum value for each element')

args = parser.parse_args()

random.seed(args.seed)

max_val = 2**64 // (args.M*args.N)
if args.max_val is not None:
    if args.max_val > max_val:
        print(f"{args.max_val=} must be no bigger than {max_val}, 2^64//(M*N)",
              file=sys.stderr)
        sys.exit(1)
    else:
        max_val = args.max_val

print(f"{args.M} {args.N}")

printedNonZero = False
for r in range(args.M):
    for c in range(args.N):
        # XXX: this method of setting the probability of zeroes is probably not sound
        if random.random() < args.zero_prob/100 and printedNonZero:
            print('0 ', end='')
        else:
            print(f"{random.randint(1, max_val)} ", end='')
            printedNonZero = True
    print()
