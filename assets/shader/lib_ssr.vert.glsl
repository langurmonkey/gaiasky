#ifdef ssrFlag
// OUTPUTS
out vec4 v_fragPosView;

void ssrData(vec4 gpos) {
    v_fragPosView = gpos;
}
#endif // ssrFlag
