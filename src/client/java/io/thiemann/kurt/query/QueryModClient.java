package io.thiemann.kurt.query;

import com.fox2code.foxloader.loader.ClientMod;

public class QueryModClient extends QueryMod implements ClientMod {
    @Override
    public void onInit() {
        this.getLogger().warning("ReIndevQuery is only needed on the server side.");
    }
}
