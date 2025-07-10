package org.hao.compiler.config.ws;

import java.security.Principal;

/**
 * TODO
 *
 * @author wanghao(helloworlwh @ 163.com)
 * @since 2025/7/10 09:50
 */
public class ObjectPrincipal<T> implements Principal {
    private T object = null;

    public ObjectPrincipal(T object) {
        this.object = object;
    }

    public T getObject() {
        return this.object;
    }

    @Override
    public String getName() {
        return this.getObject().toString();
    }

    public int hashCode() {
        return this.object.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof ObjectPrincipal) {
            ObjectPrincipal op = (ObjectPrincipal) o;
            return this.getObject().equals(op.getObject());
        } else {
            return false;
        }
    }

    public String toString() {
        return this.object.toString();
    }


}
