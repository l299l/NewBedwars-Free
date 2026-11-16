package com.l299l.newbedwars.api.shop;

/**
 * Result of a {@link ShopEditor} change.
 *
 * @param code   what happened
 * @param detail for {@link Code#INVALID} the field and reason (e.g. {@code material: unknown material FOO}),
 *               for {@link Code#IN_USE} where it is used, comma separated, for {@link Code#FAILED} the error,
 *               otherwise {@code null}
 * @since 2.7-beta
 */
public record ShopResult(Code code, String detail) {
    public enum Code {
        OK,
        /** No item or GUI with that name. */
        NOT_FOUND,
        /** A value is not valid; see {@link #detail()}. */
        INVALID,
        /** The item or GUI is still used, so it can't be deleted. */
        IN_USE,
        /** The arena is not waiting. */
        BUSY,
        /** An item or GUI with that name already exists. */
        NAME_TAKEN,
        /** Something went wrong; see {@link #detail()} and the console. */
        FAILED
    }

    public boolean isSuccess() {
        return code == Code.OK;
    }
}
