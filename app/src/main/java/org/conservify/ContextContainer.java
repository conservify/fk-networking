package org.conservify;

import android.content.Context;

public class ContextContainer {
    private static Context context;

    public ContextContainer(Context context) {
        if (context != null) {
            if (ContextContainer.context != null) {
                throw new RuntimeException("multiple contexts disallowed");
            }
            ContextContainer .context = context;
        }
    }

    public Context getContext() {
        return context;
    }
}
