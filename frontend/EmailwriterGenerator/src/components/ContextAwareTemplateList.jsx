import React, { useState } from 'react';
import {
    Box, Typography, TextField, Button, Grid, Card, CardContent, CircularProgress, Chip
} from '@mui/material';

const ContextAwareTemplateList = () => {
    const [context, setContext] = useState('');
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleAnalyze = async () => {
        setLoading(true);
        try {
            const res = await fetch('http://localhost:8080/api/templates/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ emailContent: context })
            });
            const data = await res.json();
            setRecommendations(data);
        } catch (err) {
            console.error('Failed to get context recommendations', err);
        } finally {
            setLoading(false);
        }
    };

    const glassStyle = {
        background: 'rgba(255, 255, 255, 0.05)',
        backdropFilter: 'blur(12px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        color: '#e0e0e0',
        padding: '24px'
    };

    return (
        <Box sx={{ p: 4, minHeight: '100vh', background: 'linear-gradient(135deg, #111 0%, #1a1a24 100%)' }}>
            <Typography variant="h3" sx={{
                color: '#fff', fontWeight: 800, mb: 2,
                textShadow: '0 4px 20px rgba(138, 43, 226, 0.4)'
            }}>
                AI Context-Aware Templating
            </Typography>
            <Typography variant="subtitle1" sx={{ color: '#aaa', mb: 4 }}>
                Paste the incoming email below. The Recommendation Engine will parse the semantics and rank the best templates.
            </Typography>

            <Grid container spacing={4}>
                <Grid item xs={12} md={6}>
                    <Card sx={glassStyle}>
                        <CardContent>
                            <TextField
                                fullWidth
                                multiline
                                rows={8}
                                variant="outlined"
                                placeholder="E.g., Can we schedule a meeting to discuss the project?"
                                value={context}
                                onChange={(e) => setContext(e.target.value)}
                                sx={{
                                    mb: 3,
                                    '& .MuiOutlinedInput-root': {
                                        color: '#fff',
                                        '& fieldset': { borderColor: 'rgba(255,255,255,0.2)' },
                                        '&:hover fieldset': { borderColor: '#8a2be2' },
                                    }
                                }}
                            />
                            <Button
                                variant="contained"
                                size="large"
                                fullWidth
                                onClick={handleAnalyze}
                                disabled={loading || !context.trim()}
                                sx={{
                                    background: 'linear-gradient(45deg, #8a2be2, #00e5ff)',
                                    fontWeight: 'bold',
                                    color: '#fff',
                                    py: 1.5,
                                    borderRadius: '8px'
                                }}
                            >
                                {loading ? <CircularProgress size={24} sx={{ color: '#fff' }} /> : 'Analyze & Recommend'}
                            </Button>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} md={6}>
                    <Typography variant="h5" sx={{ color: '#fff', mb: 3 }}>
                        Engine Recommendations
                    </Typography>
                    {recommendations.length === 0 && !loading && (
                        <Box sx={{ ...glassStyle, textAlign: 'center', py: 8 }}>
                            <Typography color="gray">Waiting for analysis...</Typography>
                        </Box>
                    )}
                    {recommendations.map((result, idx) => (
                        <Card key={idx} sx={{ ...glassStyle, mb: 2, p: 2, transition: '0.3s', '&:hover': { transform: 'translateX(5px)' } }}>
                            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                                <Typography variant="h6" sx={{ color: '#00e5ff', fontWeight: 'bold' }}>
                                    {result.template.title}
                                </Typography>
                                <Chip
                                    label={`Match Score: ${Math.round(result.score * 10) / 10}`}
                                    sx={{ bgcolor: 'rgba(0, 229, 255, 0.2)', color: '#00e5ff', fontWeight: 'bold' }}
                                />
                            </Box>
                            <Typography variant="body2" sx={{ color: '#ccc', fontStyle: 'italic', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                                {result.template.body}
                            </Typography>
                        </Card>
                    ))}
                </Grid>
            </Grid>
        </Box>
    );
};

export default ContextAwareTemplateList;
