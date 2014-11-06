package com.zvikabh.supermarket;

import java.io.Serializable;

public class Product implements Serializable {
	private static final long serialVersionUID = 5278202351620390038L;

	public Product(String barCode, String name, String manufacturer) {
		this.barCode = barCode;
		this.name = name;
		this.manufacturer = manufacturer;
	}

	@Override
	public String toString() {
		return name + " by " + manufacturer;
	}

	protected String barCode;
	protected String name;
	protected String manufacturer;
}
