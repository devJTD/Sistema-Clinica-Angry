document.addEventListener("DOMContentLoaded", () => {
    const especialidadSelect = document.getElementById("idEspecialidad");
    const medicoSelect = document.getElementById("idMedico");
    const fechaInput = document.getElementById("fechaCita");
    const horaSelect = document.getElementById("horaCita");

    // Clear previous options and disable initially
    function resetMedicoFechaHora() {
        medicoSelect.innerHTML = '<option value="">Selecciona un médico</option>';
        medicoSelect.disabled = true;
        fechaInput.disabled = true;
        fechaInput.value = ''; // Clear date
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';
        horaSelect.disabled = true;
    }

    // Function to load specialties
    const loadEspecialidades = async () => {
        try {
            const response = await fetch("/api/especialidades");
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            data.forEach((esp) => {
                const option = document.createElement("option");
                option.value = esp.id;
                option.textContent = esp.nombre;
                especialidadSelect.appendChild(option);
            });
        } catch (error) {
            console.error("Error al cargar especialidades:", error);
            // Optionally, display an error message to the user
            alert("No se pudieron cargar las especialidades. Por favor, intenta de nuevo más tarde.");
        }
    };

    // Function to load doctors based on specialty
    const loadMedicos = async (idEspecialidad) => {
        resetMedicoFechaHora(); // Reset lower fields when specialty changes
        if (!idEspecialidad) return;

        try {
            const response = await fetch(`/api/medicos-por-especialidad?idEspecialidad=${idEspecialidad}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            data.forEach((med) => {
                const option = document.createElement("option");
                option.value = med.id;
                option.textContent = `${med.nombre} ${med.apellido}`; // Assuming medico has nombre and apellido
                medicoSelect.appendChild(option);
            });
            medicoSelect.disabled = false;
        } catch (error) {
            console.error("Error al cargar médicos:", error);
            alert("No se pudieron cargar los médicos para esta especialidad. Por favor, intenta de nuevo.");
        }
    };

    // Function to load available hours based on doctor and date
    const loadHorarios = async (idMedico, fechaCita) => {
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>'; // Clear current hours
        horaSelect.disabled = true; // Disable until loaded

        if (!idMedico || !fechaCita) return;

        try {
            const response = await fetch(`/api/horarios-disponibles?idMedico=${idMedico}&fechaCita=${fechaCita}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            
            if (data && data.length > 0) {
                data.forEach((horario) => {
                    const option = document.createElement("option");
                    option.value = horario.hora; // Assuming horario object has a 'hora' field
                    option.textContent = horario.hora;
                    horaSelect.appendChild(option);
                });
                horaSelect.disabled = false;
            } else {
                horaSelect.innerHTML = '<option value="">No hay horas disponibles</option>';
            }
        } catch (error) {
            console.error("Error al cargar horarios:", error);
            alert("No se pudieron cargar los horarios disponibles. Por favor, intenta de nuevo.");
        }
    };

    // Event Listeners
    especialidadSelect.addEventListener("change", () => {
        const idEspecialidad = especialidadSelect.value;
        loadMedicos(idEspecialidad);
    });

    medicoSelect.addEventListener("change", () => {
        // Only enable date if a doctor is selected
        fechaInput.disabled = !medicoSelect.value;
        fechaInput.value = ''; // Clear date when doctor changes
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';
        horaSelect.disabled = true;
    });

    fechaInput.addEventListener("change", () => {
        const idMedico = medicoSelect.value;
        const fechaSeleccionada = fechaInput.value;
        loadHorarios(idMedico, fechaSeleccionada);
    });

    // Initial load
    loadEspecialidades(); // Load specialties when the page loads

    // Modal functions (these remain the same)
    window.mostrarConfirmacion = function() {
        // Basic validation before showing modal
        if (!especialidadSelect.value || !medicoSelect.value || !fechaInput.value || !horaSelect.value) {
            alert("Por favor, completa todos los campos antes de confirmar la cita.");
            return;
        }
        document.getElementById("modalConfirmacion").style.display = "block";
    };

    window.cerrarModal = function() {
        document.getElementById("modalConfirmacion").style.display = "none";
    };

    window.confirmarCita = function() {
        document.querySelector('.reserva-form').submit();
    };
});