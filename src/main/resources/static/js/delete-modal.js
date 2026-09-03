(() => {
    const modal = document.querySelector("[data-delete-modal]");

    if (!modal) {
        return;
    }

    const productNameElement = modal.querySelector("[data-delete-product-name]");
    const cancelButton = modal.querySelector("[data-delete-cancel]");
    const confirmButton = modal.querySelector("[data-delete-confirm]");
    let selectedForm = null;
    let triggerButton = null;

    const openModal = (button) => {
        selectedForm = document.getElementById(button.dataset.deleteFormId);
        triggerButton = button;

        if (!selectedForm) {
            return;
        }

        const productName = button.dataset.productName?.trim();
        productNameElement.textContent = productName ? `"${productName}"` : "este produto";
        modal.hidden = false;
        confirmButton.focus();
    };

    const closeModal = () => {
        modal.hidden = true;
        selectedForm = null;

        if (triggerButton) {
            triggerButton.focus();
            triggerButton = null;
        }
    };

    document.querySelectorAll("[data-delete-trigger]").forEach((button) => {
        button.addEventListener("click", () => openModal(button));
    });

    cancelButton.addEventListener("click", closeModal);

    confirmButton.addEventListener("click", () => {
        if (selectedForm) {
            selectedForm.requestSubmit();
        }
    });

    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) {
            closeModal();
        }
    });
})();
