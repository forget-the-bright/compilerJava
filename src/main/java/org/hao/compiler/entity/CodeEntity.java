package org.hao.compiler.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/6/20 14:00
 */
@Data
@EqualsAndHashCode
public class CodeEntity {
    private String code;
    private String className;
    private String runMethod;
}
