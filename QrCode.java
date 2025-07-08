package com.esotar.api.utils;

import java.util.*;
import java.util.regex.Pattern;

public class QrCode {

    private int versionNumber;
    private int maskPattern;
    private int errorCorrectionLevelIndex;
    private int[] errorCorrectionLevelType;
    private int[][] qrCodeMatrix;
    private int[][] modulePlacementFlags;
    private static final Pattern PATTERN_NUMERIC = Pattern.compile("^[0-9]*$");
    private static final Pattern PATTERN_ALPHA_NUMERIC = Pattern.compile("^[A-Z0-9 \\$%*+\\./:-]*$");
    private static final String BYTE_PATTERN_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
    private static final int[][] GALOIS_FIELD_POLYNOMIALS = {
            {-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
            {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},
            {-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
            {-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}
    };
    private static final int[][] ERROR_CORRECTION_CODEWORDS_PER_BLOCK = {
            {-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25},
            {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},
            {-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},
            {-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81}
    };

    private final Map<String, int[]> ecl = new HashMap<>();

    public String generateSvg(String value, int dimension, String logoLink) {

        StringBuilder stringBuilder = new StringBuilder();

        ecl.put("L", new int[]{0, 1});
        ecl.put("M", new int[]{1, 0});
        ecl.put("Q", new int[]{2, 3});
        ecl.put("H", new int[]{3, 2});

        int size = dimension > 0 ? dimension : 256;
        int pending = 1;

        //start
        encodeData(value, new int[]{1, 0}, 0, 2);

        int v = errorCorrectionLevelIndex + 2 * pending;

        int imageSize = v / 5; // Tamanho da imagem
        int imageX = (v - imageSize) / 2;
        int imageY = (v - imageSize) / 2;

        String img = logoLink == null ? "" : logoLink.isEmpty() ? "" : "<image xlink:href=" + logoLink + " height=" + imageSize + " width=" + imageSize + " x=" + imageX + " y=" + imageY + " preserveAspectRatio=none></image>";

        int i = imageSize + 2;
        int xy = imageX -2;

        for (int y = 0; y < errorCorrectionLevelIndex; y++) {
            for (int x = 0; x < errorCorrectionLevelIndex; x++) {
                if (qrCodeMatrix[y][x] != 0) {
                    if ((x < xy || i + xy <= x) || (y < xy || i + xy <= y) || img.isEmpty()){
                        stringBuilder.append("M").append(x).append(",").append(y).append("h1v1h-1v-1z");
                    }
                }
            }
        }

        // Build the SVG element as a string.
        String viewBox = "0 0 " + v + " " + v;

        String transform = "matrix(1,0,0,1," + pending + "," + pending + ")";

        String path = "<path transform=\"" + transform + "\" d=\"" + stringBuilder + "\" />";

        return "<svg  preserveAspectRatio=\"none\" viewBox=\"" + viewBox + "\" width=\"" + size +
                "\" height=\"" + size +
                "\" fill=\"" + "#000" +
                "\" shape-rendering=\"crispEdges\" xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">" +
                path + img +
                "</svg>";
    }

    private int galoisFieldMultiply(int r, int n) {
        int tempValue = 0;
        for (int bitIndex = 8; bitIndex > 0; bitIndex--) {
            tempValue = (tempValue << 1) ^ (285 * (tempValue >>> 7)) ^ (((n >>> (bitIndex - 1)) & 1) * r);
        }
        return tempValue;
    }

    private int[] calculateErrorCorrectionCodewords(int[] dataCodewords, int[] polynomialCoefficients) {
        List<Integer> list = new ArrayList<>();
        int codewordLength = dataCodewords.length;
        int remainingCodewords = codewordLength;
        while (remainingCodewords > 0) {
            int currentDataCodeword = dataCodewords[codewordLength - remainingCodewords] ^ (list.isEmpty() ? 0 : list.remove(0));
            for (int coefficientIndex = 0; coefficientIndex < polynomialCoefficients.length; coefficientIndex++) {
                int currentListValue = (coefficientIndex < list.size() ? list.get(coefficientIndex) : 0);
                if (coefficientIndex < list.size()) {
                    list.set(coefficientIndex, currentListValue ^ galoisFieldMultiply(polynomialCoefficients[coefficientIndex], currentDataCodeword));
                } else {
                    list.add(galoisFieldMultiply(polynomialCoefficients[coefficientIndex], currentDataCodeword));
                }
            }
            remainingCodewords--;
        }
        int[] errorCorrectionCodewords = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            errorCorrectionCodewords[i] = list.get(i);

        }
        return errorCorrectionCodewords;
    }

    private void applyMaskPattern(int maskPattern) {
        for (int i = 0; i < errorCorrectionLevelIndex; i++) {
            for (int j = 0; j < errorCorrectionLevelIndex; j++) {
                if (modulePlacementFlags[i][j] == 0) {
                    boolean maskBit = applyMask(maskPattern, i, j);
                    qrCodeMatrix[i][j] ^= (maskBit ? 1 : 0);
                }
            }
        }
    }

    private int calculatePenaltyScore() {
        int score = 0;
        int fArea = errorCorrectionLevelIndex * errorCorrectionLevelIndex;
        int darkCount = 0;
        for (int i = 0; i < errorCorrectionLevelIndex; i++) {
            int countH = 0;
            int countV = 0;

            for (int j = 0; j < errorCorrectionLevelIndex; j++) {
                if (qrCodeMatrix[i][j] == 1) darkCount++;

                if (j == 0 || qrCodeMatrix[i][j] == qrCodeMatrix[i][j - 1]) {
                    countH++;
                } else {
                    if (countH >= 5) {
                        score += (countH == 5 ? 3 : 1);
                    }
                    countH = 1;
                }
                if (i == 0 || qrCodeMatrix[i][j] == qrCodeMatrix[i - 1][j]) {
                    countV++;
                } else {
                    if (countV >= 5) {
                        score += (countV == 5 ? 3 : 1);
                    }
                    countV = 1;
                }
                if (i > 0 && j > 0 &&
                        qrCodeMatrix[i][j] == qrCodeMatrix[i][j - 1] &&
                        qrCodeMatrix[i][j] == qrCodeMatrix[i - 1][j] &&
                        qrCodeMatrix[i][j] == qrCodeMatrix[i - 1][j - 1]) {
                    score += 3;
                }
            }
            if (countH >= 5) score += (countH == 5 ? 3 : 1);
            if (countV >= 5) score += (countV == 5 ? 3 : 1);
        }
        score += 10 * ((int) Math.ceil(Math.abs(20 * darkCount - 10 * fArea) / (double) fArea) - 1);

        return score;
    }

    private void appendBits(int value, int bitLength, List<Integer> bitList) {
        for (int i = bitLength - 1; i >= 0; i--) {
            bitList.add((value >>> i) & 1);
        }
    }

    private int getCharacterCountBitsLength(BitData bitData, int version) {
        return bitData.numBitsCharCount[(version + 7) / 17];
    }

    private boolean isBitSet(int value, int bitIndex) {
        return 0 != (value >>> bitIndex & 1);
    }

    private int calculateTotalDataBits(List<BitData> bitDataSegments, int version) {
        int totalBits = 0;
        for (int o = bitDataSegments.size() - 1; o >= 0; o--) {
            BitData e = bitDataSegments.get(o);
            int characterCountBits = getCharacterCountBitsLength(e, version);
            if (1 << characterCountBits <= e.numChars) {
                return Integer.MAX_VALUE;
            }
            totalBits += 4 + characterCountBits + e.bitData.size();
        }
        return totalBits;

    }

    private int getDataCapacityBits(int version) {

        if (version < 1 || version > 40) {
            throw new RuntimeException("Version number out of range");
        }
        int capacity = (16 * version + 128) * version + 64;

        if (version >= 2) {
            int t = version / 7 | 2;
            capacity -= (25 * t - 10) * t - 55;
            if (version >= 7) {
                capacity -= 36;
            }
        }
        return capacity;
    }

    private void placeFunctionPatternRectangle(int row, int col) {
        for (int rowOffset = 2; -2 <= rowOffset; rowOffset--) {
            for (int colOffset = 2; -2 <= colOffset; colOffset--) {
                try {
                    setModule(row + colOffset, col + rowOffset, 1 != Math.max(Math.abs(colOffset), Math.abs(rowOffset)));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void placeFunctionPatternSquare(int row, int col) {
        for (int rowOffset = 4; -4 <= rowOffset; rowOffset--) {
            for (int colOffset = 4; -4 <= colOffset; colOffset--) {
                int a = Math.max(Math.abs(colOffset), Math.abs(rowOffset)), f = row + colOffset, u = col + rowOffset;
                if (0 <= f && f < errorCorrectionLevelIndex && 0 <= u && u < errorCorrectionLevelIndex) {
                    setModule(f, u, 2 != a && 4 != a);
                }
            }
        }
    }

    private void placeFormatInformation(int maskPattern) {
        int formatInfo = 10;
        int encodedFormatInfo;
        int polynomialValue;
        for (encodedFormatInfo = (errorCorrectionLevelType[1] << 3) | maskPattern, polynomialValue = encodedFormatInfo; formatInfo > 0; formatInfo--) {
            polynomialValue = polynomialValue << 1 ^ 1335 * (polynomialValue >>> 9);
        }

        int finalFormatInfo = 21522 ^ (encodedFormatInfo << 10 | polynomialValue);

        if (finalFormatInfo >>> 15 != 0) {
            throw new RuntimeException("Assertion error");
        }
        for (formatInfo = 0; formatInfo <= 5; formatInfo++) {
            setModule(8, formatInfo, isBitSet(finalFormatInfo, formatInfo));
        }

        setModule(8, 7, isBitSet(finalFormatInfo, 6));
        setModule(8, 8, isBitSet(finalFormatInfo, 7));
        setModule(7, 8, isBitSet(finalFormatInfo, 8));

        for (formatInfo = 9; formatInfo < 15; formatInfo++) {
            setModule(14 - formatInfo, 8, isBitSet(finalFormatInfo, formatInfo));
        }
        for (formatInfo = 0; formatInfo < 8; formatInfo++) {
            setModule(errorCorrectionLevelIndex - 1 - formatInfo, 8, isBitSet(finalFormatInfo, formatInfo));
        }
        for (formatInfo = 8; formatInfo < 15; formatInfo++) {
            setModule(8, errorCorrectionLevelIndex - 15 + formatInfo, isBitSet(finalFormatInfo, formatInfo));
        }
        setModule(8, errorCorrectionLevelIndex - 8, true);
    }

    private void placeFunctionPatterns() {
        int row;
        for (row = errorCorrectionLevelIndex - 1; row >= 0; row--) {
            setModule(6, row, 0 == row % 2);
            setModule(row, 6, 0 == row % 2);
        }

        int patternCount = 2 + (versionNumber / 7);

        int[] patternPositions = new int[patternCount];

        if (versionNumber > 1) {

            int offset = (versionNumber == 32) ? 26 : 2 * ((int) Math.ceil((errorCorrectionLevelIndex - 13) / (double) (2 * patternCount - 2)));

            for (; patternCount > 0; patternCount--) {
                int index = patternCount - 1;

                patternPositions[index] = index * offset + 6;
            }
        }


        int patternRowIndex;
        for (patternRowIndex = row = patternPositions.length - 1; patternRowIndex >= 0; patternRowIndex--) {
            for (int a = row; a >= 0; a--) {
                if (!(0 == a && 0 == patternRowIndex) && !(0 == a && patternRowIndex == row) && !(a == row && 0 == patternRowIndex)) {
                    placeFunctionPatternRectangle(patternPositions[patternRowIndex], patternPositions[a]);
                }
            }
        }

        placeFunctionPatternSquare(3, 3);
        placeFunctionPatternSquare(errorCorrectionLevelIndex - 4, 3);
        placeFunctionPatternSquare(3, errorCorrectionLevelIndex - 4);
        placeFormatInformation(0);

        if (!(7 > versionNumber)) {
            int versionInfoPolynomial = 12;
            int encodedVersionInfo;
            for (encodedVersionInfo = versionNumber; versionInfoPolynomial > 0; --versionInfoPolynomial) {
                encodedVersionInfo = encodedVersionInfo << 1 ^ 7973 * (encodedVersionInfo >>> 11);
            }

            var versionInformation = versionNumber << 12 | encodedVersionInfo;

            versionInfoPolynomial = 17;
            if (versionInformation >>> 18 != 0) {
                throw new RuntimeException("Assertion error");
            }
            for (; versionInfoPolynomial >= 0; versionInfoPolynomial--) {
                int a = errorCorrectionLevelIndex - 11 + versionInfoPolynomial % 3, f = versionInfoPolynomial / 3;
                boolean i = isBitSet(versionInformation, versionInfoPolynomial);
                setModule(a, f, i);
                setModule(f, a, i);
            }
        }
    }

    private int[] generateReedSolomonCodewords(int[] dataCodewords) {

        if (dataCodewords.length != getDataCodeWordCount(versionNumber, errorCorrectionLevelType)) {
            throw new RuntimeException("Invalid argument");
        }

        int blocksInGroup1 = ERROR_CORRECTION_CODEWORDS_PER_BLOCK[errorCorrectionLevelType[0]][versionNumber];
        int errorCorrectionBlocksLength = GALOIS_FIELD_POLYNOMIALS[errorCorrectionLevelType[0]][versionNumber];
        int dataCapacityBytes = getDataCapacityBits(versionNumber) / 8;
        int blocksInGroup2 = blocksInGroup1 - dataCapacityBytes % blocksInGroup1;
        int dataBlocksPerGroup = dataCapacityBytes / blocksInGroup1;

        int[][] blocks = new int[][]{};

        int dataCodewordIndex = 1;
        int[] t = new int[errorCorrectionBlocksLength];
        t[errorCorrectionBlocksLength - 1] = 1;

        for (int blockIndex = 0; blockIndex < errorCorrectionBlocksLength; blockIndex++) {

            for (int i2 = 0; i2 < errorCorrectionBlocksLength; i2++) {
                try {
                    t[i2] = galoisFieldMultiply(t[i2], dataCodewordIndex) ^ t[i2 + 1];
                } catch (Exception ex) {
                    t[i2] = galoisFieldMultiply(t[i2], dataCodewordIndex);
                }
            }
            dataCodewordIndex = galoisFieldMultiply(dataCodewordIndex, 2);
        }

        for (int blockIndex = 0, dataCodewordInde = 0; blockIndex < blocksInGroup1; blockIndex++) {
            int[] s = Arrays.stream(dataCodewords, dataCodewordInde, dataCodewordInde + dataBlocksPerGroup - errorCorrectionBlocksLength + (blockIndex < blocksInGroup2 ? 0 : 1)).toArray();

            dataCodewordInde += s.length;

            int[] l = calculateErrorCorrectionCodewords(s, t);

            if (blockIndex < blocksInGroup2) {
                s = Arrays.copyOf(s, s.length + 1);
                s[s.length - 1] = 0;


            }
            blocks = Arrays.copyOf(blocks, blocks.length + 1);
            blocks[blocks.length - 1] = concat(s, l);


        }

        int[] interLeaved = new int[]{};
        for (int codewordPart = 0; codewordPart < blocks[0].length; codewordPart++) {
            for (int blockPart = 0; blockPart < blocks.length; blockPart++) {
                if (codewordPart != dataBlocksPerGroup - errorCorrectionBlocksLength || blockPart >= blocksInGroup2) {
                    interLeaved = Arrays.copyOf(interLeaved, interLeaved.length + 1);
                    interLeaved[interLeaved.length - 1] = blocks[blockPart][codewordPart];
                }
            }
        }
        return interLeaved;
    }

    private int[] parseByteData(String byteData) {

        List<Integer> list = new ArrayList<>();
        for (int charIndex = 0; charIndex < byteData.length(); charIndex++) {
            if (byteData.charAt(charIndex) != '%') {
                list.add((int) byteData.charAt(charIndex));
            } else {
                String subst = byteData.substring(charIndex + 1, 2);
                list.add(Integer.parseInt(subst, 16));
                charIndex += 2;
            }
        }
        int[] byteArray = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            byteArray[i] = list.get(i);
        }

        return byteArray;
    }

    private int getDataCodeWordCount(int version, int[] errorCorrectionLevel) {
        return (getDataCapacityBits(version) / 8) - GALOIS_FIELD_POLYNOMIALS[errorCorrectionLevel[0]][version] * ERROR_CORRECTION_CODEWORDS_PER_BLOCK[errorCorrectionLevel[0]][version];
    }

    private void setModule(int col, int row, boolean isBlack) {
        qrCodeMatrix[row][col] = isBlack ? 1 : 0;
        modulePlacementFlags[row][col] = 1;
    }

    private BitData encodeByteMode(int[] data) {
        List<Integer> bitList = new ArrayList<>();
        int byteIndex = 0;
        for (; byteIndex < data.length; byteIndex++) {
            var byteValue = data[byteIndex];
            appendBits(byteValue, 8, bitList);
        }

        return new BitData(4, new int[]{8, 16, 16}, data.length, bitList);
    }

    private BitData encodeNumericMode(String data) {

        if (!PATTERN_NUMERIC.matcher(data).matches()) {
            throw new RuntimeException("String contains non-numeric characters");
        }

        List<Integer> bitList = new ArrayList<>();

        int charIndex = 0;
        for (; charIndex < data.length(); ) {
            var digitGroupLength = Math.min(data.length() - charIndex, 3);
            appendBits(Integer.parseInt(data.substring(charIndex, charIndex + digitGroupLength), 10), 3 * digitGroupLength + 1, bitList);
            charIndex += digitGroupLength;
        }

        return new BitData(1, new int[]{10, 12, 14}, data.length(), bitList);
    }

    private BitData encodeAlphanumericMode(String data) {

        if (!PATTERN_ALPHA_NUMERIC.matcher(data).matches()) {
            throw new RuntimeException("String contains unencodable characters in alphanumeric mode");
        }

        List<Integer> bitList = new ArrayList<>();
        int charIndex;
        for (charIndex = 0; charIndex + 2 <= data.length(); charIndex += 2) {
            var combinedValue = 45 * BYTE_PATTERN_CHARACTERS.indexOf(data.charAt(charIndex));
            combinedValue += BYTE_PATTERN_CHARACTERS.indexOf(data.charAt(charIndex + 1));
            appendBits(combinedValue, 11, bitList);

        }
        if (charIndex < data.length()) {
            appendBits(BYTE_PATTERN_CHARACTERS.indexOf(data.charAt(charIndex)), 6, bitList);
        }
        return new BitData(2, new int[]{9, 11, 13}, data.length(), bitList);
    }

    private void encodeData(String data, int[] errorCorrectionLevelPreference, int optimizeErrorCorrectionLevel, int preferredMaskPattern) {
        List<BitData> dataSegments = new ArrayList<>();

        if (!data.isEmpty()) {
            if (PATTERN_NUMERIC.matcher(data).matches()) {
                dataSegments.add(encodeNumericMode(data));
            } else {
                if (PATTERN_ALPHA_NUMERIC.matcher(data).matches()) {
                    dataSegments.add(encodeAlphanumericMode(data));
                } else {
                    dataSegments.add(encodeByteMode(parseByteData(data)));
                }
            }
        }

        encodeQrCodeData(dataSegments, errorCorrectionLevelPreference, optimizeErrorCorrectionLevel, preferredMaskPattern, 0, 0);
    }

    private void buildQrCodeMatrix(int version, int[] errorCorrectionLevel, int[] encodedData, int pattern) {
        errorCorrectionLevelType = errorCorrectionLevel;
        this.maskPattern = pattern;

        int matrixDimension = errorCorrectionLevelIndex = 4 * (versionNumber = version) + 17;
        qrCodeMatrix = new int[matrixDimension][matrixDimension];
        modulePlacementFlags = new int[matrixDimension][matrixDimension];

        for (; matrixDimension > 0; matrixDimension--) {
            Arrays.fill(qrCodeMatrix[matrixDimension - 1], 0);
            Arrays.fill(modulePlacementFlags[matrixDimension - 1], 0);
        }

        placeFunctionPatterns();

        var codewords = generateReedSolomonCodewords(encodedData);

        int dataCodewordIndex = 0, direction = 1, o2 = errorCorrectionLevelIndex - 1, i2 = o2;
        for (; i2 > 0; i2 -= 2) {

            if (6 == i2) {
                --i2;
            }
            int u3 = 0 > (direction = -direction) ? o2 : 0, h3 = 0;
            for (; h3 < errorCorrectionLevelIndex; ++h3) {

                for (int v4 = i2; v4 > i2 - 2; --v4) {

                    if (modulePlacementFlags[u3][v4] != 1) {

                        try {
                            qrCodeMatrix[u3][v4] = isBitSet(codewords[dataCodewordIndex >>> 3], 7 - (7 & dataCodewordIndex)) ? 1 : 0;
                        } catch (Exception ignored) {

                        }
                        ++dataCodewordIndex;
                    }


                }
                u3 += direction;
            }
        }

        if (this.maskPattern > 0) {
            var c = 1e9;
            matrixDimension = 7;
            for (; matrixDimension >= 0; matrixDimension--) {
                applyMaskPattern(matrixDimension);
                placeFormatInformation(matrixDimension);
                var s = calculatePenaltyScore();
                if (c > s) {
                    c = s;
                    this.maskPattern = matrixDimension;
                }
                applyMaskPattern(matrixDimension);

            }
        }
        applyMaskPattern(this.maskPattern);
        placeFormatInformation(this.maskPattern);
        modulePlacementFlags = new int[][]{};

    }

    private void encodeQrCodeData(List<BitData> dataSegments, int[] errorCorrectionLevelPreference, int optimizeErrorCorrectionLevel, int preferredMaskPattern, int minVersion, int maxVersion) {
        if (minVersion == 0) {
            minVersion = 1;
        }
        if (maxVersion == 0) {
            maxVersion = 40;
        }
        if (preferredMaskPattern == 0) {
            preferredMaskPattern = 0;
        }
        if (optimizeErrorCorrectionLevel == 0) {
            optimizeErrorCorrectionLevel = 1;
        }
        if (!(1 <= minVersion && minVersion <= maxVersion && maxVersion <= 40) || preferredMaskPattern < -1 || preferredMaskPattern > 7) {
            throw new RuntimeException("Invalid value");
        }

        List<Integer> bitStream = new ArrayList<>();

        int bestVersion = 236, v = minVersion;
        int requiredBits;
        while (true) {
            requiredBits = calculateTotalDataBits(dataSegments, v);
            if (requiredBits <= 8 * getDataCodeWordCount(v, errorCorrectionLevelPreference)) {
                break;
            }
            if (v >= maxVersion) {
                throw new RuntimeException("Data too long");
            }
            v++;
        }

        int[][] errorCorrectionOptions = new int[][]{ecl.get("H"), ecl.get("Q"), ecl.get("M")};

        if (optimizeErrorCorrectionLevel > 0) {
            for (int s = errorCorrectionOptions.length - 1; s >= 0; s--) {
                if (requiredBits <= 8 * getDataCodeWordCount(v, errorCorrectionOptions[s])) {
                    errorCorrectionLevelPreference = errorCorrectionOptions[s];
                }

            }
        }

        for (int segmentIndex = 0; segmentIndex < dataSegments.size(); segmentIndex++) {
            var segment = dataSegments.get(segmentIndex);
            appendBits(segment.modeBits, 4, bitStream);
            appendBits(segment.numChars, getCharacterCountBitsLength(segment, v), bitStream);
            var dataBits = segment.bitData;

            bitStream.addAll(dataBits);

        }

        if (bitStream.size() != requiredBits) {
            throw new RuntimeException("Assertion error");
        }

        var totalCapacityBits = 8 * getDataCodeWordCount(v, errorCorrectionLevelPreference);

        if (bitStream.size() > totalCapacityBits) {
            throw new RuntimeException("Assertion error");
        }

        appendBits(0, Math.min(4, totalCapacityBits - bitStream.size()), bitStream);
        appendBits(0, (8 - bitStream.size() % 8) % 8, bitStream);


        if (bitStream.size() % 8 != 0) {
            throw new RuntimeException("Assertion error");
        }

        for (; bitStream.size() < totalCapacityBits; ) {
            appendBits(bestVersion, 8, bitStream);
            bestVersion ^= 253;
        }

        int[] dataBytes = new int[bitStream.size() / 8];
        for (int s = bitStream.size() - 1; s >= 0; s--) {
            dataBytes[s >>> 3] |= bitStream.get(s) << 7 - (7 & s);
        }

        buildQrCodeMatrix(v, errorCorrectionLevelPreference, dataBytes, preferredMaskPattern);
    }

    private int[] concat(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private boolean applyMask(int maskPattern, int t, int o) {
        switch (maskPattern) {
            case 0:
                return ((t + o) % 2) == 0;
            case 1:
                return (t % 2) == 0;
            case 2:
                return (o % 3) == 0;
            case 3:
                return ((t + o) % 3) == 0;
            case 4:
                return (((t / 2) + (o / 3)) % 2) == 0;
            case 5:
                return ((t * o) % 2 + (t * o) % 3) == 0;
            case 6:
                return (((t * o) % 2 + (t * o) % 3) % 2) == 0;
            case 7:
                return (((t + o) % 2 + (t * o) % 3) % 2) == 0;
            default:
                return false;
        }
    }

    public static class BitData {
        public int modeBits;
        public int[] numBitsCharCount;
        public int numChars;
        public List<Integer> bitData;

        public BitData(int modeBits, int[] numBitsCharCount, int numChars, List<Integer> bitData) {
            this.modeBits = modeBits;
            this.numBitsCharCount = numBitsCharCount;
            this.numChars = numChars;
            this.bitData = bitData;
        }

    }

}
