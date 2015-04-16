package org.apache.celix.calc.api;

/**
 * @author abroekhuis
 *
 * Simple Calculator service to demo interoperability with Apache Celix RSA implementation.
 */
public interface Calculator {
	    double add(double a, double b);
	    double sub(double a, double b);
	    double sqrt(double a);
}
