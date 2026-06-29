import CONFIG from '../config.js';

// Base de Datos Mock Inicial (Carga si no existe en localStorage)
const MOCK_DATA_DEFAULTS = {
    usuarios: [
        { idUsuario: 1, username: 'admin', nombre: 'Administrador General', rol: 'ADMIN', password: 'admin123', activo: 1 },
        { idUsuario: 2, username: 'cajero1', nombre: 'Carlos Cajero', rol: 'CAJERO', password: 'cajero123', activo: 1 }
    ],
    categorias: [
        { idCategoria: 1, nombre: 'Bebidas', descripcion: 'Gaseosas, aguas y jugos', activo: 1 },
        { idCategoria: 2, nombre: 'Snacks', descripcion: 'Chocolates, papas y galletas', activo: 1 },
        { idCategoria: 3, nombre: 'Abarrotes', descripcion: 'Arroz, azúcar y aceites', activo: 1 },
        { idCategoria: 4, nombre: 'Limpieza', descripcion: 'Detergentes y jabones', activo: 1 }
    ],
    proveedores: [
        { idProveedor: 1, ruc: '20102030401', nombre: 'Distribuidora Alfa S.A.', contacto: 'Marcos Díaz', telefono: '988777666', correo: 'ventas@alfa.com', direccion: 'Av. Industrial 450', activo: 1 },
        { idProveedor: 2, ruc: '20504030201', nombre: 'Corporación Mayorista Beta', contacto: 'Sara Gómez', telefono: '955444333', correo: 'contacto@beta.pe', direccion: 'Jr. Comercio 789', activo: 1 }
    ],
    productos: [
        { idProducto: 1, codigo: '7750123456789', nombre: 'Coca Cola Personal 500ml', descripcion: 'Bebida gasificada de extractos vegetales', precioCompra: 1.80, precioVenta: 2.50, stock: 45, stockMinimo: 10, idCategoria: 1, idProveedor: 1, activo: 1, fechaRegistro: new Date().toISOString() },
        { idProducto: 2, codigo: '7750123456790', nombre: 'Inca Kola 1.5L', descripcion: 'Bebida gasificada sabor original', precioCompra: 3.50, precioVenta: 5.20, stock: 22, stockMinimo: 8, idCategoria: 1, idProveedor: 1, activo: 1, fechaRegistro: new Date().toISOString() },
        { idProducto: 3, codigo: '7750234567890', nombre: 'Galletas Soda Field Paquete', descripcion: 'Galleta de soda clásica salada', precioCompra: 0.40, precioVenta: 0.80, stock: 120, stockMinimo: 15, idCategoria: 2, idProveedor: 2, activo: 1, fechaRegistro: new Date().toISOString() },
        { idProducto: 4, codigo: '7750234567891', nombre: 'Papas Lays Clásicas 80g', descripcion: 'Papas fritas hojuelas crujientes', precioCompra: 1.50, precioVenta: 2.80, stock: 4, stockMinimo: 8, idCategoria: 2, idProveedor: 2, activo: 1, fechaRegistro: new Date().toISOString() }, // Bajo Stock
        { idProducto: 5, codigo: '7750345678901', nombre: 'Arroz Costeño Familiar 1kg', descripcion: 'Arroz extra seleccionado', precioCompra: 3.20, precioVenta: 4.50, stock: 3, stockMinimo: 10, idCategoria: 3, idProveedor: 1, activo: 1, fechaRegistro: new Date().toISOString() }, // Bajo Stock
        { idProducto: 6, codigo: '7750345678902', nombre: 'Fideos Don Vittorio Spaguetti 1kg', descripcion: 'Fideos de sémola de trigo duro', precioCompra: 2.10, precioVenta: 3.20, stock: 40, stockMinimo: 10, idCategoria: 3, idProveedor: 1, activo: 1, fechaRegistro: new Date().toISOString() }
    ],
    clientes: [
        { idCliente: 1, tipoDoc: 'DNI', numeroDoc: '00000000', nombre: 'Clientes Varios', telefono: '', correo: '', direccion: '', activo: 1 },
        { idCliente: 2, tipoDoc: 'DNI', numeroDoc: '12345678', nombre: 'Juan Pérez', telefono: '987654321', correo: 'juan@gmail.com', direccion: 'Av. Larco 123, Miraflores', activo: 1 },
        { idCliente: 3, tipoDoc: 'RUC', numeroDoc: '20555444332', nombre: 'Empresa XYZ S.A.C.', telefono: '01445566', correo: 'facturacion@xyz.com', direccion: 'Calle Las Camelias 456, San Isidro', activo: 1 }
    ],
    ventas: []
};

// Generar ventas iniciales para pruebas de dashboard/crosstab (últimos 3 meses)
function generarVentasIniciales() {
    const ventas = [];
    const productos = MOCK_DATA_DEFAULTS.productos;
    const clientes = MOCK_DATA_DEFAULTS.clientes;
    const metodos = ['EFECTIVO', 'TARJETA'];
    const tipos = ['BOLETA', 'FACTURA'];
    
    let consecutivo = 1;
    const hoy = new Date();
    
    // Generar unas 15 ventas distribuidas en los últimos 45 días
    for (let i = 45; i >= 0; i -= 3) {
        const fechaVenta = new Date();
        fechaVenta.setDate(hoy.getDate() - i);
        fechaVenta.setHours(9 + (consecutivo % 8), 15 * (consecutivo % 4), 0);
        
        const clienteIdx = (consecutivo % 2) + 1; // cliente 1 (público) o 2 (Juan Perez)
        const cliente = clientes[clienteIdx];
        
        // Items de la venta
        const numItems = (consecutivo % 3) + 1;
        const detalles = [];
        let subtotal = 0;
        
        for (let j = 0; j < numItems; j++) {
            const prodIdx = (consecutivo + j) % productos.length;
            const prod = productos[prodIdx];
            const cant = (consecutivo % 2) + 1;
            const precioUnit = prod.precioVenta;
            const desc = 0;
            const itemSub = cant * precioUnit;
            
            detalles.push({
                idProducto: prod.idProducto,
                nombreProducto: prod.nombre,
                idCategoria: prod.idCategoria,
                cantidad: cant,
                precioUnit: precioUnit,
                descuento: desc,
                subtotal: itemSub
            });
            subtotal += itemSub;
        }
        
        const igv = parseFloat((subtotal * 0.18).toFixed(2));
        const total = parseFloat((subtotal + igv).toFixed(2));
        
        ventas.push({
            idVenta: consecutivo,
            numeroComprobante: `C${tipos[consecutivo % 2] === 'BOLETA' ? 'B' : 'F'}-${String(consecutivo).padStart(6, '0')}`,
            tipoComprobante: tipos[consecutivo % 2],
            idCliente: cliente.idCliente,
            nombreCliente: cliente.nombre,
            idUsuario: (consecutivo % 2) + 1,
            nombreCajero: (consecutivo % 2 === 0) ? 'Carlos Cajero' : 'Administrador General',
            fechaVenta: fechaVenta.toISOString(),
            subtotal: parseFloat(subtotal.toFixed(2)),
            igv: igv,
            descuento: 0,
            total: total,
            metodoPago: metodos[consecutivo % metodos.length],
            estado: 'COMPLETADA',
            detalles: detalles
        });
        
        consecutivo++;
    }
    return ventas;
}

// Inicializar base de datos local
function initLocalDatabase() {
    for (const key in MOCK_DATA_DEFAULTS) {
        if (!localStorage.getItem(`pukio_${key}`)) {
            if (key === 'ventas') {
                localStorage.setItem(`pukio_ventas`, JSON.stringify(generarVentasIniciales()));
            } else {
                localStorage.setItem(`pukio_${key}`, JSON.stringify(MOCK_DATA_DEFAULTS[key]));
            }
        }
    }
}

// Cargar DB local al importar api.js
if (CONFIG.MOCK_MODE) {
    initLocalDatabase();
}

// Funciones Auxiliares de LocalStorage DB
const db = {
    get: (key) => JSON.parse(localStorage.getItem(`pukio_${key}`)),
    set: (key, data) => localStorage.setItem(`pukio_${key}`, JSON.stringify(data))
};

// Router y Handler Mock para simular la API REST
async function handleMockRequest(method, url, data = null) {
    await new Promise(resolve => setTimeout(resolve, CONFIG.MOCK_DELAY));
    
    // Obtener ruta relativa y parámetros de búsqueda
    const urlObj = new URL(url, window.location.origin);
    const path = urlObj.pathname.replace(/^\/api\//, ''); // Quita '/api/'
    const query = Object.fromEntries(urlObj.searchParams.entries());
    
    // ----------------------------------------------------
    // AUTHENTICATION
    // ----------------------------------------------------
    if (path === 'auth/login' && method === 'POST') {
        const { username, password } = data;
        const usuarios = db.get('usuarios');
        const user = usuarios.find(u => u.username === username && u.activo === 1);
        if (!user || user.password !== password) {
            throw new Error('Usuario o contraseña incorrectos.');
        }
        // Retorna datos del usuario autenticado (quitando el password)
        const { password: _, ...safeUser } = user;
        return safeUser;
    }
    
    // ----------------------------------------------------
    // PRODUCTOS
    // ----------------------------------------------------
    if (path === 'productos') {
        const productos = db.get('productos');
        const categorias = db.get('categorias');
        const proveedores = db.get('proveedores');
        
        // Mapear info de categoría y proveedor en los productos
        const mappedProds = productos.map(p => ({
            ...p,
            categoriaNombre: categorias.find(c => c.idCategoria === p.idCategoria)?.nombre || 'Sin Categoría',
            proveedorNombre: proveedores.find(pr => pr.idProveedor === p.idProveedor)?.nombre || 'Sin Proveedor'
        }));
        
        if (method === 'GET') {
            if (query.id) {
                const p = mappedProds.find(item => item.idProducto === parseInt(query.id));
                if (!p) throw new Error('Producto no encontrado.');
                return p;
            }
            if (query.codigo) {
                const p = mappedProds.find(item => item.codigo === query.codigo && item.activo === 1);
                if (!p) throw new Error('Producto no encontrado.');
                return p;
            }
            return mappedProds.filter(p => p.activo === 1);
        }
        
        if (method === 'POST') {
            const list = db.get('productos');
            // Validaciones
            if (list.some(p => p.codigo === data.codigo && p.activo === 1)) {
                throw new Error('El código de producto ya está registrado.');
            }
            const newProd = {
                idProducto: list.length > 0 ? Math.max(...list.map(p => p.idProducto)) + 1 : 1,
                ...data,
                activo: 1,
                fechaRegistro: new Date().toISOString()
            };
            list.push(newProd);
            db.set('productos', list);
            return newProd;
        }
        
        if (method === 'PUT') {
            const list = db.get('productos');
            const idx = list.findIndex(p => p.idProducto === parseInt(data.idProducto));
            if (idx === -1) throw new Error('Producto no encontrado para actualizar.');
            
            // Validar que no choque el código
            if (list.some(p => p.codigo === data.codigo && p.idProducto !== parseInt(data.idProducto) && p.activo === 1)) {
                throw new Error('El código de barras ya pertenece a otro producto.');
            }
            
            list[idx] = { ...list[idx], ...data };
            db.set('productos', list);
            return list[idx];
        }
        
        if (method === 'DELETE') {
            const id = parseInt(query.id);
            const list = db.get('productos');
            const idx = list.findIndex(p => p.idProducto === id);
            if (idx === -1) throw new Error('Producto no encontrado.');
            
            list[idx].activo = 0; // Desactivación lógica
            db.set('productos', list);
            return { success: true, message: 'Producto desactivado correctamente.' };
        }
    }
    
    if (path === 'productos/buscar' && method === 'GET') {
        const q = (query.q || '').toLowerCase();
        const productos = db.get('productos').filter(p => p.activo === 1);
        const categorias = db.get('categorias');
        const proveedores = db.get('proveedores');
        
        const mapped = productos.map(p => ({
            ...p,
            categoriaNombre: categorias.find(c => c.idCategoria === p.idCategoria)?.nombre || '',
            proveedorNombre: proveedores.find(pr => pr.idProveedor === p.idProveedor)?.nombre || ''
        }));
        
        return mapped.filter(p => 
            p.nombre.toLowerCase().includes(q) || 
            p.codigo.includes(q) ||
            p.categoriaNombre.toLowerCase().includes(q)
        );
    }
    
    if (path === 'productos/bajo-stock' && method === 'GET') {
        const productos = db.get('productos').filter(p => p.activo === 1 && p.stock <= p.stockMinimo);
        const categorias = db.get('categorias');
        return productos.map(p => ({
            ...p,
            categoriaNombre: categorias.find(c => c.idCategoria === p.idCategoria)?.nombre || ''
        }));
    }
    
    // ----------------------------------------------------
    // CATEGORIAS
    // ----------------------------------------------------
    if (path === 'categorias' && method === 'GET') {
        return db.get('categorias').filter(c => c.activo === 1);
    }
    
    // ----------------------------------------------------
    // PROVEEDORES
    // ----------------------------------------------------
    if (path === 'proveedores' && method === 'GET') {
        return db.get('proveedores').filter(p => p.activo === 1);
    }
    
    // ----------------------------------------------------
    // CLIENTES
    // ----------------------------------------------------
    if (path === 'clientes') {
        const clientes = db.get('clientes');
        if (method === 'GET') {
            return clientes.filter(c => c.activo === 1);
        }
        if (method === 'POST') {
            if (clientes.some(c => c.numeroDoc === data.numeroDoc && c.activo === 1)) {
                throw new Error('El documento de cliente ya está registrado.');
            }
            const newCli = {
                idCliente: clientes.length > 0 ? Math.max(...clientes.map(c => c.idCliente)) + 1 : 1,
                ...data,
                activo: 1,
                fechaRegistro: new Date().toISOString()
            };
            clientes.push(newCli);
            db.set('clientes', clientes);
            return newCli;
        }
        if (method === 'PUT') {
            const idx = clientes.findIndex(c => c.idCliente === parseInt(data.idCliente));
            if (idx === -1) throw new Error('Cliente no encontrado.');
            
            if (clientes.some(c => c.numeroDoc === data.numeroDoc && c.idCliente !== parseInt(data.idCliente) && c.activo === 1)) {
                throw new Error('El documento ya está registrado por otro cliente.');
            }
            
            clientes[idx] = { ...clientes[idx], ...data };
            db.set('clientes', clientes);
            return clientes[idx];
        }
        if (method === 'DELETE') {
            const id = parseInt(query.id);
            const idx = clientes.findIndex(c => c.idCliente === id);
            if (idx === -1) throw new Error('Cliente no encontrado.');
            if (id === 1) throw new Error('No se puede desactivar al cliente genérico.');
            
            clientes[idx].activo = 0;
            db.set('clientes', clientes);
            return { success: true };
        }
    }
    
    if (path === 'clientes/buscar' && method === 'GET') {
        const q = (query.q || '').toLowerCase();
        const clientes = db.get('clientes').filter(c => c.activo === 1);
        return clientes.filter(c => 
            c.nombre.toLowerCase().includes(q) || 
            c.numeroDoc.includes(q)
        );
    }
    
    // ----------------------------------------------------
    // VENTAS
    // ----------------------------------------------------
    if (path === 'ventas') {
        const ventas = db.get('ventas');
        if (method === 'GET') {
            return ventas;
        }
        if (method === 'POST') {
            const list = db.get('ventas');
            const productos = db.get('productos');
            
            // Reducir stock de los productos vendidos
            data.detalles.forEach(item => {
                const prod = productos.find(p => p.idProducto === item.idProducto);
                if (prod) {
                    if (prod.stock < item.cantidad) {
                        throw new Error(`Stock insuficiente para el producto: ${prod.nombre}. Disponible: ${prod.stock}`);
                    }
                    prod.stock -= item.cantidad;
                }
            });
            db.set('productos', productos);
            
            // Guardar venta
            const consecutivo = list.length + 1;
            const prefijo = data.tipoComprobante === 'BOLETA' ? 'B' : 'F';
            const numCompr = `C${prefijo}-${String(consecutivo).padStart(6, '0')}`;
            
            const newVenta = {
                idVenta: consecutivo,
                numeroComprobante: numCompr,
                fechaVenta: new Date().toISOString(),
                estado: 'COMPLETADA',
                ...data
            };
            list.push(newVenta);
            db.set('ventas', list);
            return newVenta;
        }
    }
    
    if (path === 'ventas/resumen-hoy' && method === 'GET') {
        const ventas = db.get('ventas');
        const hoyStr = new Date().toDateString();
        
        const ventasHoy = ventas.filter(v => new Date(v.fechaVenta).toDateString() === hoyStr);
        const subtotal = ventasHoy.reduce((acc, v) => acc + v.subtotal, 0);
        const igv = ventasHoy.reduce((acc, v) => acc + v.igv, 0);
        const total = ventasHoy.reduce((acc, v) => acc + v.total, 0);
        const cantidad = ventasHoy.length;
        
        return {
            subtotal: parseFloat(subtotal.toFixed(2)),
            igv: parseFloat(igv.toFixed(2)),
            total: parseFloat(total.toFixed(2)),
            cantidad: cantidad
        };
    }
    
    // ----------------------------------------------------
    // REPORTES Y OLAP
    // ----------------------------------------------------
    if (path === 'reportes/ventas' && method === 'GET') {
        const ventas = db.get('ventas');
        const desde = query.desde ? new Date(query.desde) : null;
        const hasta = query.hasta ? new Date(query.hasta) : null;
        
        if (hasta) hasta.setHours(23, 59, 59, 999); // Todo el día
        
        return ventas.filter(v => {
            const f = new Date(v.fechaVenta);
            if (desde && f < desde) return false;
            if (hasta && f > hasta) return false;
            return true;
        });
    }
    
    if (path === 'reportes/productos-top' && method === 'GET') {
        const limit = parseInt(query.limit) || 5;
        const ventas = db.get('ventas');
        
        const prodsVendidos = {};
        ventas.forEach(v => {
            v.detalles.forEach(d => {
                if (!prodsVendidos[d.idProducto]) {
                    prodsVendidos[d.idProducto] = {
                        idProducto: d.idProducto,
                        nombre: d.nombreProducto,
                        cantidadTotal: 0,
                        totalIngresos: 0
                    };
                }
                prodsVendidos[d.idProducto].cantidadTotal += d.cantidad;
                prodsVendidos[d.idProducto].totalIngresos += d.subtotal;
            });
        });
        
        return Object.values(prodsVendidos)
            .sort((a, b) => b.cantidadTotal - a.cantidadTotal)
            .slice(0, limit);
    }
    
    if (path === 'dwh/procesar' && method === 'POST') {
        // En modo mock, simplemente indicamos éxito (los datos ya están en localStorage)
        return { success: true, rowsDwh: 0, rowsOlap: 0, message: 'DataWarehouse y Cubo OLAP actualizados (modo simulación).' };
    }

    if (path === 'dwh/crosstab' && method === 'GET') {
        const ventas = db.get('ventas');
        const categorias = db.get('categorias');
        
        // Estructura del crosstab: Agrupar por año, mes y categoría
        const agrupado = {};
        
        ventas.forEach(v => {
            const fecha = new Date(v.fechaVenta);
            const anio = fecha.getFullYear();
            const mes = fecha.getMonth() + 1; // 1-12
            
            v.detalles.forEach(d => {
                const catNombre = categorias.find(c => c.idCategoria === d.idCategoria)?.nombre || 'Otros';
                const key = `${anio}-${mes}-${catNombre}`;
                
                if (!agrupado[key]) {
                    agrupado[key] = {
                        anio: anio,
                        mes: mes,
                        categoria: catNombre,
                        totalUnidades: 0,
                        totalIngresos: 0,
                        totalVentas: 0,
                        ventasContadas: new Set()
                    };
                }
                agrupado[key].totalUnidades += d.cantidad;
                agrupado[key].totalIngresos += d.subtotal;
                agrupado[key].ventasContadas.add(v.idVenta);
            });
        });
        
        // Mapear al formato esperado por el frontend
        const crossTabArray = Object.values(agrupado).map(item => {
            const { ventasContadas, ...rest } = item;
            return {
                ...rest,
                totalVentas: ventasContadas.size,
                totalIngresos: parseFloat(rest.totalIngresos.toFixed(2))
            };
        });
        
        // Ordenar por año desc, mes desc, categoría asc
        return crossTabArray.sort((a, b) => {
            if (b.anio !== a.anio) return b.anio - a.anio;
            if (b.mes !== a.mes) return b.mes - a.mes;
            return a.categoria.localeCompare(b.categoria);
        });
    }

    // ----------------------------------------------------
    // USUARIOS (solo ADMIN — RF-51, RF-52, RF-53)
    // ----------------------------------------------------
    if (path === 'usuarios') {
        const usuarios = db.get('usuarios');
        if (method === 'GET') {
            return usuarios.map(({ password: _, ...u }) => u);
        }
        if (method === 'POST') {
            const existe = usuarios.find(u => u.username === data.username);
            if (existe) throw new Error('El nombre de usuario ya existe. Elija otro.');
            const nuevo = {
                idUsuario: Date.now(),
                username:  data.username,
                nombre:    data.nombre,
                rol:       data.rol || 'CAJERO',
                password:  data.password,
                activo:    1
            };
            usuarios.push(nuevo);
            db.set('usuarios', usuarios);
            return { success: true, message: 'Usuario registrado correctamente.' };
        }
        if (method === 'PUT') {
            const idx = usuarios.findIndex(u => u.idUsuario === data.idUsuario);
            if (idx === -1) throw new Error('Usuario no encontrado.');
            usuarios[idx] = { ...usuarios[idx], nombre: data.nombre, rol: data.rol, activo: data.activo ? 1 : 0 };
            db.set('usuarios', usuarios);
            return { success: true, message: 'Usuario actualizado correctamente.' };
        }
    }

    if (path === 'usuarios/password' && method === 'PUT') {
        const usuarios = db.get('usuarios');
        const idx = usuarios.findIndex(u => u.idUsuario === data.idUsuario);
        if (idx === -1) throw new Error('Usuario no encontrado.');
        usuarios[idx].password = data.password;
        db.set('usuarios', usuarios);
        return { success: true, message: 'Contraseña actualizada correctamente.' };
    }

    throw new Error(`Endpoint no soportado en modo simulación: ${method} ${path}`);
}

// Cliente de API HTTP que conmuta entre llamadas reales y mockeadas
const API = {
    async request(url, options = {}) {
        const path = url.startsWith('/') ? url.substring(1) : url;
        const fullUrl = CONFIG.MOCK_MODE ? `/api/${path}` : `${CONFIG.API_BASE_URL}/${path}`;
        const method = options.method || 'GET';
        
        if (CONFIG.MOCK_MODE) {
            try {
                const body = options.body ? JSON.parse(options.body) : null;
                return await handleMockRequest(method, fullUrl, body);
            } catch (err) {
                console.error(`[MOCK API ERROR] ${method} ${fullUrl}:`, err);
                throw err;
            }
        }
        
        // Modo HTTP real (para integrar con PHP)
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };
        
        // Añadir cabecera de autenticación si el token existe
        const user = JSON.parse(localStorage.getItem('pukio_sesion'));
        if (user && user.token) {
            headers['Authorization'] = `Bearer ${user.token}`;
        }
        
        const response = await fetch(fullUrl, {
            ...options,
            headers,
            credentials: 'include'
        });
        
        const responseData = await response.json().catch(() => null);
        
        if (!response.ok) {
            const errorMsg = (responseData && (responseData.error || responseData.message)) || `Error HTTP ${response.status}: ${response.statusText}`;
            if (response.status === 401) {
                // Sesión expirada: limpiar sesión y redirigir al login
                localStorage.removeItem('pukio_sesion');
                window.location.href = '/frontend/index.html';
            }
            if (response.status === 403) {
                // Acceso denegado por rol insuficiente: lanzar error descriptivo sin redirigir
                throw new Error(errorMsg || 'Acceso denegado. Se requiere rol ADMIN para esta operación.');
            }
            throw new Error(errorMsg);
        }
        
        return responseData;
    },
    
    get(url, searchParams = null) {
        let fullUrl = url;
        if (searchParams) {
            const q = new URLSearchParams(searchParams).toString();
            fullUrl += `?${q}`;
        }
        return this.request(fullUrl, { method: 'GET' });
    },
    
    post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },
    
    put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },
    
    delete(url, params = null) {
        let fullUrl = url;
        if (params) {
            const q = new URLSearchParams(params).toString();
            fullUrl += `?${q}`;
        }
        return this.request(fullUrl, { method: 'DELETE' });
    }
};

export default API;
