package s0553449;

class FuzzyLogic {

    public static float clamp(float a) {
        return Math.min(Math.max(a, 0), 1);
    }

    public static float not(float a) {
        return 1f - a;
    }

    public static float and(float a, float b) {
        return Math.min(a, b);
    }

    public static float or(float a, float b) {
        return Math.max(a, b);
    }

    public static float fuzzLinear(float value, float low, float high) {
        return clamp((value - low) / (high - low));
    }

    public static float defuzzLinear(float fuzzyValue, float low, float high) {
        return low + fuzzyValue * (high - low);
    }
}