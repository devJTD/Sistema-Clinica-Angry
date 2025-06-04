document.addEventListener("DOMContentLoaded", () => {
  const especialidadSelect = document.getElementById("idEspecialidad");
  const medicoSelect = document.getElementById("idMedico");
  const fechaInput = document.getElementById("fechaCita");
  const horaSelect = document.getElementById("horaCita");

  let medicos = [];
  let horarios = [];

  // Cargar especialidades
  fetch("/data/especialidades.json")
    .then((res) => res.json())
    .then((data) => {
      data.forEach((esp) => {
        const option = document.createElement("option");
        option.value = esp.id;
        option.textContent = esp.nombre;
        especialidadSelect.appendChild(option);
      });
    });

  // Al seleccionar especialidad, cargar médicos
  especialidadSelect.addEventListener("change", () => {
    const idEspecialidad = especialidadSelect.value;
    medicoSelect.innerHTML = '<option value="">Selecciona un médico</option>';
    fechaInput.disabled = true;
    horaSelect.disabled = true;
    horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';

    if (!idEspecialidad) {
      medicoSelect.disabled = true;
      return;
    }

    fetch("/data/medicos.json")
      .then((res) => res.json())
      .then((data) => {
        medicos = data;
        const filtrados = medicos.filter(
          (m) => m.especialidad.id == idEspecialidad
        );
        filtrados.forEach((med) => {
          const option = document.createElement("option");
          option.value = med.id;
          option.textContent = med.nombreCompleto;
          medicoSelect.appendChild(option);
        });
        medicoSelect.disabled = false;
      });
  });

  // Al seleccionar médico, habilitar fecha
  medicoSelect.addEventListener("change", () => {
    fechaInput.disabled = !medicoSelect.value;
    horaSelect.disabled = true;
    horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';
  });

  fechaInput.addEventListener("change", () => {
    const idMedico = parseInt(medicoSelect.value);
    const fechaSeleccionada = fechaInput.value;

    horaSelect.innerHTML = '<option value="">Selecciona una hora</option>';

    if (!idMedico || !fechaSeleccionada) {
      horaSelect.disabled = true;
      return;
    }

    fetch("/data/horarios.json")
      .then((res) => res.json())
      .then((data) => {
        const entrada = data.find(
          (h) => h.medicoId === idMedico && h.fecha === fechaSeleccionada
        );

        if (entrada && Array.isArray(entrada.horas)) {
          const horasDisponibles = entrada.horas.filter((h) => h.disponible);

          horasDisponibles.forEach((horario) => {
            const option = document.createElement("option");
            option.value = horario.hora;
            option.textContent = horario.hora;
            horaSelect.appendChild(option);
          });

          horaSelect.disabled = horasDisponibles.length === 0;
        } else {
          horaSelect.disabled = true;
        }
      });
  });
});

function mostrarConfirmacion() {
  document.getElementById("modalConfirmacion").style.display = "block";
}

function cerrarModal() {
  document.getElementById("modalConfirmacion").style.display = "none";
}

function confirmarCita() {
    document.querySelector('.reserva-form').submit();
}

