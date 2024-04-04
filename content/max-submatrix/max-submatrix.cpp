#include <algorithm>
#include <cstdint>
#include <format>
#include <iostream>
#include <vector>

int main() {
    int M, N;

    std::cin >> M >> N;

    std::vector<std::vector<uint64_t>>
        A(M, std::vector<uint64_t>(N)),
        S(M, std::vector<uint64_t>(N)),
        D(M, std::vector<uint64_t>(N));

    for (int i=0; i<M; i++)
        for (int j=0; j<N; j++)
            std::cin >> A[i][j];

    for (int i=0; i<M; i++)
        for (int j=0; j<N; j++) {
            S[i][j] = A[i][j];

            if (i == 0 && j == 0)
                continue;

            if (i > 0)
                S[i][j] += S[i-1][j];

            if (j > 0)
                S[i][j] += S[i][j-1];

            if (i > 0 && j > 0)
                S[i][j] -= S[i-1][j-1];
        }

    for (int i=0; i<M; i++)
        for (int j=0; j<N; j++)
            if (A[i][j] == 0)
                D[i][j] = 0;
            else {
                D[i][j] = 1;

                if (i == 0 || j == 0)
                    continue;

                D[i][j] += std::min({D[i-1][j], D[i][j-1], D[i-1][j-1]});
            }

    uint64_t maxSum = 0;
    int maxI = -1, maxJ = -1;

    for (int i=0; i<M; i++)
        for (int j=0; j<N; j++) {
            // Coordinates of *the upper left neighbor* of the upper left corner
            // of the widest nonzero contiguous square submatrix with a lower
            // right corner at (i,j)
            int ul_i = i - D[i][j];
            int ul_j = j - D[i][j];

            uint64_t sum = S[i][j];

            if (ul_i >= 0)
                sum -= S[ul_i][j];

            if (ul_j >= 0)
                sum -= S[i][ul_j];

            if (ul_i >= 0 && ul_j >= 0)
                sum += S[ul_i][ul_j];

            if (sum > maxSum) {
                maxSum = sum;
                maxI = i;
                maxJ = j;
            }
        }

    std::cout << std::format("Sum={} in {}-wide square starting at ({},{})\n",
        maxSum,
        D[maxI][maxJ],
        maxI - D[maxI][maxJ] + 1,
        maxJ - D[maxI][maxJ] + 1);
}
