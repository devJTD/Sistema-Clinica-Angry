document.addEventListener("DOMContentLoaded", () => {
    const especialidadSelect = document.getElementById("idEspecialidad");
    const medicoSelect = document.getElementById("idMedico");
    const fechaInput = document.getElementById("fechaCita");
    const horaSelect = document.getElementById("horaCita");

    // Función para obtener la fecha actual en formato YYYY-MM-DD
    const getTodayDate = () => {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0'); // Meses son 0-11
        const day = String(today.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    // Establece la fecha mínima para el campo de fecha
    const setMinDate = () => {
        fechaInput.min = getTodayDate();
    };

    // Limpiar opciones previas y deshabilitar inicialmente
    function resetMedicoFechaHora() {
        medicoSelect.innerHTML = '<option value="">Selecciona un médico</option>';
        medicoSelect.disabled = true;
        fechaInput.disabled = true;
        fechaInput.value = '';
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';
        horaSelect.disabled = true;
    }

    // Función para cargar especialidades
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
            alert("No se pudieron cargar las especialidades. Por favor, intenta de nuevo más tarde.");
        }
    };

    // Función para cargar médicos según la especialidad
    const loadMedicos = async (idEspecialidad) => {
        resetMedicoFechaHora(); // Resetear campos inferiores cuando la especialidad cambia
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
                option.textContent = `${med.nombre} ${med.apellido}`; // Asumiendo que el médico tiene nombre y apellido
                medicoSelect.appendChild(option);
            });
            medicoSelect.disabled = false;
        } catch (error) {
            console.error("Error al cargar médicos:", error);
            alert("No se pudieron cargar los médicos para esta especialidad. Por favor, intenta de nuevo.");
        }
    };

    // Función para cargar horarios disponibles según el médico y la fecha
    const loadHorarios = async (idMedico, fechaCita) => {
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>'; // Limpiar horas actuales
        horaSelect.disabled = true; // Deshabilitar hasta que se carguen

        if (!idMedico || !fechaCita) return;

        try {
            const response = await fetch(`/api/horarios-disponibles?idMedico=${idMedico}&fechaCita=${fechaCita}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();

            // Obtener la fecha y hora actual
            const now = new Date();
            const todayDateString = getTodayDate(); // Reutiliza la función que ya tienes

            // Si la fecha seleccionada es hoy, filtra los horarios pasados
            const isToday = fechaCita === todayDateString;

            const availableHorarios = data.filter((horario) => {
                if (isToday) {
                    // Combina la fecha actual con la hora del horario para crear un objeto Date completo
                    const [hour, minute] = horario.hora.split(':').map(Number);
                    const horarioDateTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute);

                    // Compara con la hora actual. Añade un pequeño margen (ej. 1 minuto) para evitar problemas con segundos exactos.
                    return horarioDateTime > now;
                }
                return true; // Si no es hoy, todos los horarios son válidos
            });

            if (availableHorarios && availableHorarios.length > 0) {
                availableHorarios.forEach((horario) => {
                    const option = document.createElement("option");
                    option.value = horario.hora;
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
        // Solo habilitar la fecha si se selecciona un médico
        fechaInput.disabled = !medicoSelect.value;
        fechaInput.value = ''; // Limpiar fecha cuando el médico cambia
        horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';
        horaSelect.disabled = true;
        // Establecer la fecha mínima cuando el campo se habilita
        if (!fechaInput.disabled) {
            setMinDate();
        }
    });

    fechaInput.addEventListener("change", () => {
        const idMedico = medicoSelect.value;
        const fechaSeleccionada = fechaInput.value;
        loadHorarios(idMedico, fechaSeleccionada);
    });

    // Carga inicial
    loadEspecialidades(); // Cargar especialidades cuando la página carga
    setMinDate(); // Establecer la fecha mínima al cargar la página por primera vez

    // Funciones del Modal (estas se mantienen igual)
    window.mostrarConfirmacion = function () {
        // Validación básica antes de mostrar el modal
        if (!especialidadSelect.value || !medicoSelect.value || !fechaInput.value || !horaSelect.value) {
            alert("Por favor, completa todos los campos antes de confirmar la cita.");
            return;
        }
        document.getElementById("modalConfirmacion").style.display = "block";
    };

    window.cerrarModal = function () {
        document.getElementById("modalConfirmacion").style.display = "none";
    };

    window.confirmarCita = function () {
        document.querySelector('.reserva-form').submit();
    };
});