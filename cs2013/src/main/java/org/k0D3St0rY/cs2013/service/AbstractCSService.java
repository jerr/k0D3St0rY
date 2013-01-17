package org.k0D3St0rY.cs2013.service;

import java.util.List;
import java.util.Map;

public abstract class AbstractCSService {
    public abstract CharSequence execute(Map<String, List<String>> params);
}
