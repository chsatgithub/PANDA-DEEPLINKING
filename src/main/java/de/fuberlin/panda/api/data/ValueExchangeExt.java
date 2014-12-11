package de.fuberlin.panda.api.data;

/*
 * #%L
 * PANDA-DEEPLINKING
 * %%
 * Copyright (C) 2014 Freie Universitaet Berlin
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Extension of auto generated ValueExchange class with custom methods.
 * 
 * @author Christoph Schröder
 */
@XmlRootElement(name = "PANDA")
public class ValueExchangeExt extends ValueExchange {

    public void addValue(Value v) {
        // discard empty values
        if (!(v.getValue() == null) && !(v.getValue().toString().isEmpty())) {
            this.getValue().add(v);
        }
    }

    public void addValues(List<Value> values) {
        for (Value v : values) {
            // discard empty values
            if (!(v.getValue() == null) && !(v.getValue().toString().isEmpty())) {
                this.getValue().add(v);
            }
        }
    }
}
