#!/usr/bin/env bash

set -e

./p2.anim.sc < input.txt > out.ppms

csplit -f frame. -b "%03d.ppm" -z out.ppms /^P/ '{*}'
rm out.ppms

convert frame.*.ppm -background black -extent 41x41 resized.%03d.ppm
rm frame.*.ppm

ffmpeg \
    -i "resized.%03d.ppm" \
    -vf "scale=iw*10:ih*10:flags=neighbor" \
    -c:v libvpx -crf 4 -b:v 100M \
    aoc.2019.15.webm
rm resized.*.ppm
