package com.sid.argline;

import java.util.Set;

public interface Argument<T> {

	T resolve(Set<Argument<?>> all);

	boolean hasParameter();

	boolean consumeParameter(String string);

	boolean satisfied();

	Object name();

	T getValue();

}
