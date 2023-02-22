package com.ugav.battalion.util;

import java.util.AbstractCollection;
import java.util.Collection;

public interface ICollection<E> extends Collection<E> {

	@Override
	public Iter<E> iterator();

	public abstract static class Abstract<E> extends AbstractCollection<E> implements ICollection<E> {
	}

}
