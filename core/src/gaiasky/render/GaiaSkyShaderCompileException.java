/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.util.SysUtils;
import gaiasky.util.gdx.shader.ComputeShaderProgram;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.TessellationShaderProgram;
import gaiasky.util.i18n.I18n;

/**
 * A shader failed to compile.
 */
public class GaiaSkyShaderCompileException extends RuntimeException {

    public GaiaSkyShaderCompileException(ComputeShaderProgram program) {
        super(getMessage(program));

        // Write all shaders.
        writeShader(program.getShaderFileName(), program.getShaderSource());

    }

    public GaiaSkyShaderCompileException(ExtShaderProgram program) {
        super(getMessage(program));

        // Write all shaders.
        writeShader(program.getVertexShaderFileName(), program.getVertexShaderSource());
        writeShader(program.getGeometryShaderFileName(), program.getGeometryShaderSource());
        writeShader(program.getFragmentShaderFileName(), program.getFragmentShaderSource());

        if (program instanceof TessellationShaderProgram tessProgram) {
            writeShader(tessProgram.getControlShaderFileName(), tessProgram.getControlShaderSource());
            writeShader(tessProgram.getEvaluationShaderFileName(), tessProgram.getEvaluationShaderSource());
        }

    }

    private static String getMessage(ComputeShaderProgram program) {
        final String nl = System.lineSeparator();
        final StringBuilder buff = new StringBuilder();
        if (program.getName() == null || program.getName().isEmpty()) {
            buff.append(I18n.msg("notif.shader.compile.fail")).append(nl);
        } else {
            buff.append(I18n.msg("notif.shader.compile.fail")).append(":: ").append(program.getName()).append(nl);
        }
        if (program.getName() != null) {
            buff.append(I18n.msg("notif.shader.compute", program.getShaderFileName())).append(nl);
        }
        buff.append(program.getLog()).append(nl);
        buff.append("Full shader saved to ").append(SysUtils.getCrashShadersDir()).append(nl);
        return buff.toString();
    }

    private static String getMessage(ExtShaderProgram program) {
        final String nl = System.lineSeparator();
        final StringBuilder buff = new StringBuilder();
        if (program.getName() == null || program.getName().isEmpty()) {
            buff.append(I18n.msg("notif.shader.compile.fail")).append(nl);
        } else {
            buff.append(I18n.msg("notif.shader.compile.fail")).append(":: ").append(program.getName()).append(nl);
        }
        if (program.getVertexShaderFileName() != null) {
            buff.append(I18n.msg("notif.shader.vertex", program.getVertexShaderFileName())).append(nl);
        }
        if (program instanceof TessellationShaderProgram tessProgram) {
            if (tessProgram.getControlShaderFileName() != null) {
                buff.append(I18n.msg("notif.shader.tessellation.control", tessProgram.getControlShaderFileName())).append(nl);
            }
            if (tessProgram.getEvaluationShaderFileName() != null) {
                buff.append(I18n.msg("notif.shader.tessellation.evaluation", tessProgram.getEvaluationShaderFileName())).append(nl);
            }
        }
        if (program.getGeometryShaderFileName() != null) {
            buff.append(I18n.msg("notif.shader.geometry", program.getGeometryShaderFileName())).append(nl);
        }
        if (program.getFragmentShaderFileName() != null) {
            buff.append(I18n.msg("notif.shader.fragment", program.getFragmentShaderFileName())).append(nl);
        }
        buff.append(program.getLog()).append(nl);
        buff.append("Full shaders saved to ").append(SysUtils.getCrashShadersDir()).append(nl);
        return buff.toString();
    }

    private void writeShader(String name, String code) {
        ExtShaderProgram.writeShader(name, code);
    }
}
