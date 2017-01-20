package com.sid.argline;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class Arguments<T> implements Argument<T> {
	private final static AtomicInteger HASHCOUNTER = new AtomicInteger(-1);

	private final static Map<Pattern, Arguments<?>> ARGUMENTS = new Hashtable<>();

	private final Pattern pattern;
	private T parameter;

	private final Function<String, T> parameterParser;

	private final String title;

	private int hash;

	private final Function<Entry<Argument<T>, T>, T> resolver;

	private Arguments(int hash, String title, Pattern patt, Function<String, T> paramParser,
			Function<Entry<Argument<T>, T>, T> resolver) {
		this.title = title;
		this.pattern = patt;
		this.parameterParser = paramParser;
		this.hash = hash;
		this.resolver = resolver;
	}

	public synchronized static <T extends Object> Arguments<T> createArgument(String title, String patt,
			Function<String, T> paramParser, Function<Entry<Argument<T>, T>, T> resolver) {
		Pattern pattern = Pattern.compile(patt);
		Arguments<T> arg = null;
		if (!ARGUMENTS.containsKey(pattern)) {
			arg = new Arguments<T>(HASHCOUNTER.getAndDecrement(), title, pattern, paramParser, resolver);
			ARGUMENTS.put(arg.pattern, arg);
		}
		return arg;
	}

	public T resolve(Set<Argument<?>> all) {
		Entry<Argument<T>, T> entry = new SimpleEntry<>(this, parameter);
		if (resolver != null) {
			entry.setValue(resolver.apply(entry));
		}
		return parameter = entry.getValue();
	}

	public static Argument<?> argumentFor(final String ag) throws CloneNotSupportedException {
		return (Argument<?>) ARGUMENTS.entrySet().stream().filter(e -> e.getKey().matcher(ag).matches()).findAny().get()
				.getValue().clone();
	}

	@Override
	public boolean hasParameter() {
		return parameterParser != null;
	}

	@Override
	public boolean consumeParameter(String value) throws Error {
		return parameterParser == null || (parameter = parameterParser.apply(value)) != null;
	}

	@Override
	public boolean satisfied() {
		return parameterParser == null || parameter != null;
	}

	@Override
	public String name() {
		return title;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Arguments<>(hash, title, pattern, parameterParser, resolver);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	public static void printUsage(PrintStream err) {
		// TODO Auto-generated method stub

	}

	public static Set<Argument<?>> parseArguments(String[] args) throws CloneNotSupportedException {
		Set<Argument<?>> list = new HashSet<Argument<?>>();
		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			Argument<?> arg = Arguments.argumentFor(ag);
			if (arg.hasParameter() && i < args.length - 1) {
				if (arg.consumeParameter(args[i + 1])) {
					i++;
				}
			}
			if (!arg.satisfied()) {
				throw new IllegalArgumentException(String.format("Incorrect value for - '%s'", arg.name()));
			}
			list.add(arg);
		}
		return list;
	}

	@Override
	public T getValue() {
		return parameter;
	}

	@Override
	public String toString() {
		return String.format("%s#%s[%s]", title, hash, parameter);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Arguments && ((Arguments<?>) obj).hash == hash;
	}

}
