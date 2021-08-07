package com.mz.jarboot.base;

import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author majianzheng
 */
public class ParamRequestCondition {
    
    private final Set<ParamExpression> expressions;
    
    public ParamRequestCondition(String... expressions) {
        this.expressions = parseExpressions(expressions);
    }
    
    private Set<ParamExpression> parseExpressions(String... params) {
        if (ObjectUtils.isEmpty(params)) {
            return Collections.emptySet();
        }
        Set<ParamExpression> exps = new LinkedHashSet<>(params.length);
        for (String param : params) {
            exps.add(new ParamExpression(param));
        }
        return exps;
    }
    
    public Set<ParamExpression> getExpressions() {
        return expressions;
    }
    
    public ParamRequestCondition getMatchingCondition(HttpServletRequest request) {
        for (ParamExpression expression : this.expressions) {
            if (!expression.match(request)) {
                return null;
            }
        }
        return this;
    }
    
    @Override
    public String toString() {
        return "ParamRequestCondition{" + "expressions=" + expressions + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParamRequestCondition that = (ParamRequestCondition) o;
        return Objects.equals(expressions, that.expressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressions);
    }

    static class ParamExpression {
        
        private final String name;
        
        private final String value;
        
        private final boolean isNegated;
        
        ParamExpression(String expression) {
            int separator = expression.indexOf('=');
            if (separator == -1) {
                this.isNegated = expression.startsWith("!");
                this.name = isNegated ? expression.substring(1) : expression;
                this.value = null;
            } else {
                this.isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
                this.name = isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator);
                this.value = expression.substring(separator + 1);
            }
        }
        
        public final boolean match(HttpServletRequest request) {
            boolean isMatch;
            if (this.value != null) {
                isMatch = matchValue(request);
            } else {
                isMatch = matchName(request);
            }
            return this.isNegated != isMatch;
        }
        
        private boolean matchName(HttpServletRequest request) {
            return request.getParameterMap().containsKey(this.name);
        }
        
        private boolean matchValue(HttpServletRequest request) {
            return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ParamExpression that = (ParamExpression) o;
            return isNegated == that.isNegated && Objects.equals(name, that.name) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, isNegated);
        }

        @Override
        public String toString() {
            return "ParamExpression{" + "name='" + name + '\'' + ", value='" + value + '\'' + ", isNegated=" + isNegated
                    + '}';
        }
    }
}
