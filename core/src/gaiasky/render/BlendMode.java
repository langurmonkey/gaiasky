package gaiasky.render;

public enum BlendMode {
    /** Uses GL_ONE and GL_ONE for source and destination in blend equation. **/
    ADDITIVE,
    /** Uses GL_SRC_ALPHA and GL_ONE_MINUS_SRC_ALPHA for source and destination in blend equation. **/
    ALPHA,
    /** Uses GL_ONE and GL_ONE_MINUS_SRC_COLOR for source and destination in blend equation. **/
    COLOR,
    /** Disable blending **/
    NONE;
}
