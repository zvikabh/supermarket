package com.zvikabh.supermarket;

import java.io.Serializable;

public abstract interface AbstractProductDatabase extends Serializable {
	/**
	 * Gets the Product with the specified barcode, or null if not found.
	 */
	public abstract Product get(String barCode);
	
	/**
	 * Adds the specified Product to the database.
	 * If the database is persistent, this process might be completed asynchronously.
	 */
	public abstract void add(Product product);
}
