package net.yellowstrawberry.ecomonic.api.logging;

public enum Actions {
    ACCESS("access"),
    DEPOSIT("deposit"),
    WITHDRAW("withdraw"),
    TRANSFER("transfer"),
    CREATE_ACCOUNT("create_account"),
    DELETE_ACCOUNT("delete_account"),
    UPDATE_ACCOUNT("update_account");

    private final String code;
    Actions(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
