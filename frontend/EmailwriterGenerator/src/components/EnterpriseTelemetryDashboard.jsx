import React, { useEffect, useState } from 'react';
import { 
    Box, Typography, CircularProgress, 
    Card, CardContent, Grid, Paper, 
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow
} from '@mui/material';

const EnterpriseTelemetryDashboard = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch('http://localhost:8080/api/telemetry/stats')
            .then(res => res.json())
            .then(data => {
                setStats(data);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setLoading(false);
            });
    }, []);

    const glassStyle = {
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(10px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        color: '#e0e0e0',
        padding: '24px'
    };

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" mt={10}>
                <CircularProgress sx={{ color: '#00e5ff' }} />
            </Box>
        );
    }

    if (!stats) {
        return <Typography color="error" variant="h6" align="center" mt={5}>Failed to load telemetry data</Typography>;
    }

    return (
        <Box sx={{ p: 4, minHeight: '100vh', background: 'linear-gradient(135deg, #09090b 0%, #17171d 100%)' }}>
            <Typography variant="h3" sx={{ color: '#fff', fontWeight: 800, mb: 4, 
                textShadow: '0 4px 20px rgba(0, 229, 255, 0.4)' }}>
                Enterprise Telemetry
            </Typography>

            <Grid container spacing={4}>
                {/* Latency Card */}
                <Grid item xs={12} md={4}>
                    <Card sx={glassStyle}>
                        <CardContent>
                            <Typography variant="subtitle1" color="gray" gutterBottom>Avg Latency (SLA)</Typography>
                            <Typography variant="h2" sx={{ fontWeight: 700, color: '#00e5ff' }}>
                                {stats.averageLatencyMs} <span style={{ fontSize: '1rem', color: '#888' }}>ms</span>
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>

                {/* P95 Card */}
                <Grid item xs={12} md={4}>
                    <Card sx={glassStyle}>
                        <CardContent>
                            <Typography variant="subtitle1" color="gray" gutterBottom>P95 Latency</Typography>
                            <Typography variant="h2" sx={{ fontWeight: 700, color: '#ff3d00' }}>
                                {stats.p95LatencyMs} <span style={{ fontSize: '1rem', color: '#888' }}>ms</span>
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>

                {/* Success Rate */}
                <Grid item xs={12} md={4}>
                    <Card sx={glassStyle}>
                        <CardContent>
                            <Typography variant="subtitle1" color="gray" gutterBottom>Success Rate</Typography>
                            <Typography variant="h2" sx={{ fontWeight: 700, color: '#00e676' }}>
                                {stats.successRate}%
                            </Typography>
                        </CardContent>
                    </Card>
                </Grid>

                {/* Provider Usage Matrix */}
                <Grid item xs={12}>
                    <Typography variant="h5" sx={{ color: '#fff', mt: 4, mb: 2 }}>Provider Distribution Matrix</Typography>
                    <TableContainer component={Paper} sx={{ ...glassStyle, padding: 0 }}>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{ color: '#888', fontWeight: 'bold' }}>LLM Provider Node</TableCell>
                                    <TableCell sx={{ color: '#888', fontWeight: 'bold' }}>Inferences Executed</TableCell>
                                    <TableCell sx={{ color: '#888', fontWeight: 'bold' }}>Status</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {Object.keys(stats.providerDistribution || {}).map((provider) => (
                                    <TableRow key={provider}>
                                        <TableCell sx={{ color: '#fff' }}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#00e5ff' }} />
                                                {provider.toUpperCase()}
                                            </Box>
                                        </TableCell>
                                        <TableCell sx={{ color: '#fff', fontSize: '1.2rem' }}>
                                            {stats.providerDistribution[provider]}
                                        </TableCell>
                                        <TableCell sx={{ color: '#00e676' }}>Active</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Grid>
            </Grid>
        </Box>
    );
};

export default EnterpriseTelemetryDashboard;
